package com.intege.mediahand.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.intege.mediahand.MediaLoader;
import com.intege.mediahand.WatchState;
import com.intege.mediahand.core.JfxMediaHandApplication;
import com.intege.mediahand.domain.MediaEntry;
import com.intege.mediahand.domain.repository.MediaEntryRepository;
import com.intege.mediahand.utils.MessageUtil;
import com.intege.mediahand.vlc.ControlPane;
import com.intege.mediahand.vlc.JavaFxMediaPlayer;
import com.intege.mediahand.vlc.JavaFxMediaTeaser;
import com.intege.mediahand.vlc.MediaPlayerContextMenu;
import com.intege.mediahand.vlc.event.StopRenderingSceneHandler;
import com.intege.utils.PipeStream;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerUnpluggedException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

@Slf4j
@Component
@FxmlView("mediaHandApp.fxml")
public class MediaHandAppController {

    public static final String THUMBNAIL_FILE_TYPE = ".jpg";

    public static final String THUMBNAILS_FOLDER = "\\thumbnails\\";

    @FXML
    public TableView<MediaEntry> mediaTableView;

    private static ObservableList<MediaEntry> mediaEntries;

    private static FilteredList<MediaEntry> filteredData;

    @FXML
    public TextField titleFilter;

    @FXML
    public ComboBox<String> watchStateFilter;

    @FXML
    public ComboBox<String> typeFilter;

    @FXML
    public CheckBox showAllCheckbox;

    @FXML
    public CheckBox autoContinueCheckbox;

    @FXML
    public ComboBox<String> watchStateEdit;

    @FXML
    public ComboBox<Integer> ratingEdit;

    @FXML
    public DatePicker watchedEdit;

    @FXML
    public ComboBox<Integer> episodeEdit;

    @FXML
    public Label selectedMediaTitle;

    @FXML
    public TableColumn<MediaEntry, String> title;

    @FXML
    public JavaFxMediaTeaser mediaTeaser;

    @FXML
    public CheckBox playTeaser;

    private CustomControllerIndex currentController;

    private boolean isRunning;

    private JavaFxMediaPlayer javaFxMediaPlayer;
    private ControlPane controlPane;
    private MediaPlayerContextMenu mediaPlayerContextMenu;

    @Autowired
    private MediaLoader mediaLoader;

    @Autowired
    private MediaEntryRepository mediaEntryRepository;

    @Lazy
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private JfxMediaHandApplication jfxMediaHandApplication;

    @Getter
    private Thread checkThumbnailOnRequestThread;

    private Stack<Integer> updateMediaTeaserStack;

    @Qualifier("thumbnailGenerationExecutor")
    @Autowired
    private TaskExecutor thumbnailExecutor;

    public void init() {
        this.updateMediaTeaserStack = new Stack<>();
        initMediaPlayer();

        startControllerListener();
        addWatchStateFilter();
        addMediaTypeFilter();
        addTitleTooltip();
        addWatchStateEditValues();
        addRatingEditValues();
        addAllListeners();
        List<MediaEntry> mediaEntries = this.mediaEntryRepository.findAll();
        //        initThumbnailGeneration(mediaEntries);
        fillTableView(mediaEntries);
    }

    private void addAllListeners() {
        addTitleFieldFilterListener();
        addMediaTableViewListener();
        addRatingEditListener();
        addEpisodeEditListener();
        addWatchStateListener();
        addWatchedEditListener();
        addPlayTeaserListener();
    }

    private void addTitleTooltip() {
        this.title.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                if (item != null) {
                    setTooltip(new Tooltip(MediaHandAppController.this.mediaEntryRepository.findByTitle(item).getAbsolutePath()));
                }
            }
        });
    }

    private void addWatchStateEditValues() {
        this.watchStateEdit.setItems(FXCollections.observableArrayList(WatchState.WANT_TO_WATCH.toString(), WatchState.DOWNLOADING.toString(), WatchState.WATCHED.toString(), WatchState.WATCHING.toString(), WatchState.REWATCHING.toString()));
    }

    private void addRatingEditValues() {
        this.ratingEdit.setItems(FXCollections.observableArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    private void addRatingEditListener() {
        this.ratingEdit.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && newValue != null && selectedItem.getRating() != newValue) {
                selectedItem.setRating(newValue);
                this.mediaEntryRepository.save(selectedItem);
                MediaHandAppController.triggerMediaEntryUpdate(selectedItem);
            }
        });
    }

    private void addEpisodeEditListener() {
        this.episodeEdit.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && newValue != null && selectedItem.getCurrentEpisode() != newValue) {
                selectedItem.setCurrentEpisode(newValue);
                this.mediaEntryRepository.save(selectedItem);
                MediaHandAppController.triggerMediaEntryUpdate(selectedItem);
            }
        });
    }

    private void addWatchStateListener() {
        this.watchStateEdit.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !Objects.equals(selectedItem.getWatchState().toString(), newValue)) {
                selectedItem.setWatchState(WatchState.lookupByName(newValue));
                this.mediaEntryRepository.save(selectedItem);
                MediaHandAppController.triggerMediaEntryUpdate(selectedItem);
            }
        });
    }

    private void addWatchedEditListener() {
        this.watchedEdit.valueProperty().addListener((observable, oldValue, newValue) -> {
            MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !Objects.equals(selectedItem.getWatchedDate(), newValue)) {
                selectedItem.setWatchedDate(newValue);
                this.mediaEntryRepository.save(selectedItem);
                MediaHandAppController.triggerMediaEntryUpdate(selectedItem);
            }
        });
    }

    private void generateThumbnails(final MediaEntry mediaEntry) {
        this.thumbnailExecutor.execute(() -> {
            if (mediaEntry.isAvailable()) {
                for (int i = 1; i <= mediaEntry.getEpisodeNumber(); i++) {
                    int finalIndex = i;
                    this.thumbnailExecutor.execute(() -> checkThumbnailForEpisode(finalIndex, mediaEntry));
                }
            }
        });
    }

    private void initThumbnailGeneration(final List<MediaEntry> mediaEntries) {
        for (MediaEntry mediaEntry : mediaEntries) {
            this.thumbnailExecutor.execute(() -> generateThumbnails(mediaEntry));
        }
    }

    private void addPlayTeaserListener() {
        this.playTeaser.selectedProperty().addListener((observable, oldValue, newValue) -> {
            MediaEntry mediaEntry = this.mediaTableView.getSelectionModel().getSelectedItem();
            if (mediaEntry != null && Files.exists(Path.of(mediaEntry.getAbsolutePath() + THUMBNAILS_FOLDER + mediaEntry.getCurrentEpisode() + THUMBNAIL_FILE_TYPE))) {
                this.mediaTeaser.getThumbnailView().setImage(new Image(
                        "file:" + mediaEntry.getAbsolutePath() + THUMBNAILS_FOLDER + mediaEntry.getCurrentEpisode() + THUMBNAIL_FILE_TYPE));
                this.mediaTeaser.switchImageView(false);
                this.mediaTeaser.pause();
            }

            handleThumbnailOnRequestThread();
        });
    }

    private void handleThumbnailOnRequestThread() {
        if (this.checkThumbnailOnRequestThread != null && this.checkThumbnailOnRequestThread.isAlive()) {
            if (this.updateMediaTeaserStack.size() < 2) {
                this.updateMediaTeaserStack.push(0);
            }
        } else {
            initUpdateMediaTeaserThread();
        }
    }

    private void initUpdateMediaTeaserThread() {
        Runnable runnable = () -> {
            this.updateMediaTeaserStack.push(0);
            while (!this.updateMediaTeaserStack.empty()) {
                updateMediaTeaser();
                this.updateMediaTeaserStack.pop();
            }
        };
        this.checkThumbnailOnRequestThread = new Thread(runnable);
        this.checkThumbnailOnRequestThread.start();
    }

    private void addMediaTableViewListener() {
        this.mediaTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.selectedMediaTitle.setText(newValue.getTitle());
                this.selectedMediaTitle.setTooltip(new Tooltip(newValue.getTitle()));
                this.watchStateEdit.getSelectionModel().select(newValue.getWatchState().toString());
                this.ratingEdit.getSelectionModel().select(newValue.getRating());
                if (newValue.getEpisodeNumber() != this.episodeEdit.getItems().size()) {
                    List<Integer> episodes = new ArrayList<>();
                    for (int i = 0; i < newValue.getEpisodeNumber(); i++) {
                        episodes.add(i + 1);
                    }
                    this.episodeEdit.getSelectionModel().select(null);
                    this.episodeEdit.setItems(FXCollections.observableArrayList(episodes));
                }
                this.episodeEdit.getSelectionModel().select(newValue.getCurrentEpisode() - 1);
                this.watchedEdit.setValue(newValue.getWatchedDate());
                if (newValue.isAvailable()) {
                    if (Files.exists(Path.of(newValue.getAbsolutePath() + THUMBNAILS_FOLDER + newValue.getCurrentEpisode() + THUMBNAIL_FILE_TYPE))) {
                        this.mediaTeaser.getThumbnailView().setImage(new Image(
                                "file:" + newValue.getAbsolutePath() + THUMBNAILS_FOLDER + newValue.getCurrentEpisode() + THUMBNAIL_FILE_TYPE));
                        this.mediaTeaser.switchImageView(false);
                        this.mediaTeaser.pause();
                    }
                    handleThumbnailOnRequestThread();
                } else {
                    this.mediaTeaser.pause();
                    this.mediaTeaser.switchImageView(false);
                    this.mediaTeaser.getThumbnailView().setVisible(false);
                }
            } else {
                this.selectedMediaTitle.setText("Selected media");
                this.selectedMediaTitle.setTooltip(new Tooltip("Selected media"));
            }
        });
    }

    private void updateMediaTeaser() {
        MediaEntry mediaEntry = this.mediaTableView.getSelectionModel().getSelectedItem();

        if (mediaEntry == null) {
            return;
        }

        if (Files.notExists(Path.of(mediaEntry.getAbsolutePath() + THUMBNAILS_FOLDER))) {
            try {
                Files.createDirectory(Path.of(mediaEntry.getAbsolutePath() + THUMBNAILS_FOLDER));
            } catch (IOException e) {
                log.error("Could not create thumbnails directory", e);
            }
        }
        if (mediaEntry.isAvailable()) {
            if (!this.playTeaser.isSelected()) {
                checkThumbnailForEpisode(mediaEntry.getCurrentEpisode(), mediaEntry);
                this.mediaTeaser.pause();
                this.mediaTeaser.switchImageView(false);
            } else {
                this.mediaTeaser.switchImageView(true);
                try {
                    if (this.mediaTeaser.start(MediaLoader.getEpisode(mediaEntry.getAbsolutePath(), mediaEntry.getCurrentEpisode()))) {
                        this.mediaTeaser.getEmbeddedMediaPlayer().audio().setTrack(-1);
                        if (!this.playTeaser.isSelected()) {
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                this.mediaTeaser.switchImageView(false);
                            }
                            this.mediaTeaser.pause();
                        }
                    }
                } catch (IOException e) {
                    this.mediaTeaser.pause();
                    this.mediaTeaser.switchImageView(false);
                }
                checkThumbnailForEpisode(mediaEntry.getCurrentEpisode(), mediaEntry);
            }
            if (mediaEntry.equals(this.mediaTableView.getSelectionModel().getSelectedItem())) {
                this.mediaTeaser.getThumbnailView().setImage(new Image(
                        "file:" + mediaEntry.getAbsolutePath() + THUMBNAILS_FOLDER + mediaEntry.getCurrentEpisode() + THUMBNAIL_FILE_TYPE));
            }
        }
    }

    /**
     * Checks if a thumbnail for {@code episodeIndex} of {@code mediaEntry} exists. Generates a new thumbnail if none exists.
     *
     * @param episodeIndex the index of the episode to check
     * @param mediaEntry the media entry to check
     * @return true, if a thumbnail for that episode exists or was successfully generated
     */
    private boolean checkThumbnailForEpisode(final int episodeIndex, final MediaEntry mediaEntry) {
        try {
            File episode = MediaLoader.getEpisode(mediaEntry.getAbsolutePath(), episodeIndex);
            if (!thumbnailExists(episodeIndex, mediaEntry)) {
                if (isCurrentSelection(episodeIndex, mediaEntry)) {
                    this.mediaTeaser.getThumbnailView().setVisible(false);
                }
                return generateThumbnail(episode, mediaEntry, episodeIndex, 3);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Could not check thumbnail", e);
            return false;
        }
        return true;
    }

    /**
     * Return true, if episode {@code episodeIndex} of {@code mediaEntry} is current selection.
     *
     * @param episodeIndex the episode index to be selected
     * @param mediaEntry the media entry to be selected
     * @return true, if given episode and media entry is selected
     */
    private boolean isCurrentSelection(final int episodeIndex, final MediaEntry mediaEntry) {
        return mediaEntry.equals(this.mediaTableView.getSelectionModel().getSelectedItem()) && episodeIndex == mediaEntry.getCurrentEpisode();
    }

    /**
     * Return true, if a thumbnail for episode {@code episodeIndex} of {@code mediaEntry} exists.
     *
     * @param episodeIndex the episode index to check for a thumbnail
     * @param mediaEntry the media entry to check
     * @return true, if a thumbnail exists
     */
    private boolean thumbnailExists(final int episodeIndex, final MediaEntry mediaEntry) {
        return Files.exists(Path.of(mediaEntry.getAbsolutePath() + THUMBNAILS_FOLDER + episodeIndex + THUMBNAIL_FILE_TYPE));
    }

    /**
     * Generate a new thumbnail for {@code episode} of {@code mediaEntry} at {@code timeInSeconds} of the video.
     *
     * @param episode the episode's file
     * @param mediaEntry the media entry
     * @param episodeIndex the episode's index
     * @param timeInSeconds the time to approximately create the thumbnail at in the video
     * @return true, if the thumbnail was created
     * @throws IOException if the generated thumbnail could not be processed successfully or the ffmpeg command could not be executed
     * @throws InterruptedException if the ffmpeg command could not be executed in time (max. 5s)
     */
    private boolean generateThumbnail(final File episode, final MediaEntry mediaEntry, final int episodeIndex, final int timeInSeconds) throws IOException, InterruptedException {
        String episodePath = episode.getAbsolutePath();
        String command = String.format("ffmpeg -ss %d -i \"%s\" -qscale:v 2 -vf \"select=gt(scene\\,0.5)\" -frames:v 3 -vsync vfr \"%s%s%s%d%s"
                + "\"", timeInSeconds, episodePath, mediaEntry.getAbsolutePath(), THUMBNAILS_FOLDER, "version_%02d_", episodeIndex, THUMBNAIL_FILE_TYPE);
        Process process = Runtime.getRuntime().exec("cmd /c " + command);

        PipeStream out = new PipeStream(process.getInputStream(), System.out);
        PipeStream in = new PipeStream(process.getErrorStream(), System.err);
        out.start();
        in.start();

        boolean exitVal = process.waitFor(5000, TimeUnit.MILLISECONDS);
        if (exitVal) {
            long size = 0;
            Path thumbnail = null;
            for (int i = 1; i <= 3; i++) {
                Path tmpThumbnail = Path.of(mediaEntry.getAbsolutePath() + THUMBNAILS_FOLDER + "version_0" + i + "_" + episodeIndex + THUMBNAIL_FILE_TYPE);
                if (Files.exists(tmpThumbnail)) {
                    if (Files.size(tmpThumbnail) > size) {
                        size = Files.size(tmpThumbnail);
                        if (thumbnail != null) {
                            Files.delete(thumbnail);
                        }
                        thumbnail = tmpThumbnail;
                    } else {
                        Files.delete(tmpThumbnail);
                    }
                }
            }
            assert thumbnail != null;
            File file = thumbnail.toFile();
            if (!file.renameTo(new File(file.getAbsolutePath().replace(file.getName(), episodeIndex + THUMBNAIL_FILE_TYPE)))) {
                Files.delete(Path.of(file.getAbsolutePath().replace(file.getName(), episodeIndex + THUMBNAIL_FILE_TYPE)));
                if (!file.renameTo(new File(file.getAbsolutePath().replace(file.getName(), episodeIndex + THUMBNAIL_FILE_TYPE)))) {
                    Files.delete(file.toPath());
                    log.error("Could not rename thumbnail for " + episodePath);
                    return false;
                }
            }

            return true;
        } else {
            log.error("Could not generate thumbnail for " + episodePath);
            return false;
        }
    }

    /**
     * Cleanup after media entry stopped playing.
     */
    public void onMediaFinished() {
        this.controlPane.stop();
        boolean fullScreen = this.jfxMediaHandApplication.getStage().isFullScreen();
        increaseCurrentEpisode();
        if (this.autoContinueCheckbox.isSelected()) {
            playEmbeddedMedia();
            this.jfxMediaHandApplication.getStage().setFullScreen(fullScreen);
        } else {
            this.jfxMediaHandApplication.setDefaultScene();
        }
    }

    /**
     * Start listener for xbox controller actions.
     */
    public void startControllerListener() {
        if (this.isRunning) {
            return;
        }
        CustomControllerManager controllerManager = new CustomControllerManager();
        controllerManager.initSDLGamepad();
        this.currentController = controllerManager.getControllerIndex(0);
        Thread thread = new Thread(() -> {
            this.isRunning = true;
            while (this.isRunning) {
                controllerManager.update();
                try {
                    if (this.currentController.isButtonJustPressed(ControllerButton.DPAD_DOWN) || (this.currentController.isButtonPressed(ControllerButton.DPAD_DOWN)
                            && this.currentController.isButtonPressed(ControllerButton.A))) {
                        Platform.runLater(() -> {
                            if (this.mediaTableView.getSelectionModel().isEmpty()) {
                                this.mediaTableView.getSelectionModel().selectFirst();
                            } else {
                                this.mediaTableView.getSelectionModel().selectNext();
                                this.mediaTableView.scrollTo(this.mediaTableView.getSelectionModel().selectedItemProperty().get());
                            }
                        });
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.DPAD_UP) || (this.currentController.isButtonPressed(ControllerButton.DPAD_UP)
                            && this.currentController.isButtonPressed(ControllerButton.A))) {
                        Platform.runLater(() -> {
                            this.mediaTableView.getSelectionModel().selectPrevious();
                            this.mediaTableView.scrollTo(this.mediaTableView.getSelectionModel().selectedItemProperty().get());
                        });
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.START)) {
                        Platform.runLater(this::playEmbeddedMedia);
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.Y)) {
                        Platform.runLater(this::increaseCurrentEpisode);
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.X)) {
                        Platform.runLater(this::decreaseCurrentEpisode);
                    }
                } catch (ControllerUnpluggedException e) {
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    MediaHandAppController.log.error("Controller thread: sleep", e);
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
    }

    public void stopControllerListener() {
        this.isRunning = false;
    }

    private void addTitleFieldFilterListener() {
        this.titleFilter.textProperty().addListener((observable, oldValue, newValue) -> MediaHandAppController.filteredData.setPredicate(m -> filter(m, newValue)));
    }

    private void addWatchStateFilter() {
        this.watchStateFilter.setItems(FXCollections.observableArrayList(WatchState.ALL.toString(), WatchState.WANT_TO_WATCH.toString(), WatchState.DOWNLOADING.toString(), WatchState.WATCHED.toString(), WatchState.WATCHING.toString(), WatchState.REWATCHING.toString()));
        this.watchStateFilter.getSelectionModel().select("ALL");
    }

    private void addMediaTypeFilter() {
        List<String> mediaTypes = new ArrayList<>(this.mediaEntryRepository.findAllMediaTypes());
        mediaTypes.add(0, "All");
        this.typeFilter.setItems(FXCollections.observableArrayList(mediaTypes));
        this.typeFilter.getSelectionModel().select("All");
    }

    public void onPlayEnter(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            playMedia();
        }
    }

    public void onPlayButton() {
        playEmbeddedMedia();
    }

    /**
     * Sort and update visible media entries of the table.
     *
     * @param mediaEntries the media entries to show within the table
     */
    public void fillTableView(List<MediaEntry> mediaEntries) {
        MediaHandAppController.mediaEntries = FXCollections.observableArrayList(mediaEntries);

        MediaHandAppController.filteredData = new FilteredList<>(MediaHandAppController.mediaEntries, this::filter);

        SortedList<MediaEntry> sortedData = new SortedList<>(MediaHandAppController.filteredData);
        sortedData.comparatorProperty().bind(this.mediaTableView.comparatorProperty());

        this.mediaTableView.setItems(sortedData);

        TableColumn<MediaEntry, ?> mediaEntryTableTitleColumn = this.mediaTableView.getColumns().get(0);
        mediaEntryTableTitleColumn.setSortType(TableColumn.SortType.ASCENDING);
        this.mediaTableView.getSortOrder().add(mediaEntryTableTitleColumn);
    }

    /**
     * Play the selected media entry's current episode with the embedded vlcj player.
     */
    public void playEmbeddedMedia() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            MessageUtil.infoAlert("Play media", "Please select a media entry.");
        } else if (!selectedItem.isAvailable() && !selectedItem.fileExists()) {
            MessageUtil.infoAlert(
                    "Play media: " + selectedItem.getAbsolutePath(), "Selected media is not available. Deselect 'Show All' to show only media of connected media directories.");
        } else {
            try {
                File file = MediaLoader.getEpisode(selectedItem.getAbsolutePath(), selectedItem.getCurrentEpisode());

                String windowTitle = selectedItem.getTitle() + " : Episode " + selectedItem.getCurrentEpisode();
                this.isRunning = false;

                this.jfxMediaHandApplication.getStage().setTitle(windowTitle);
                this.jfxMediaHandApplication.getStage().setScene(this.javaFxMediaPlayer.getScene());

                if (this.javaFxMediaPlayer.start(file)) {
                    this.mediaTeaser.pause();
                    this.mediaTeaser.switchImageView(false);
                    this.controlPane.update(selectedItem);
                    this.mediaPlayerContextMenu.update(selectedItem);
                } else {
                    MessageUtil.warningAlert("Play embedded media failed", "Could not play selected entry " + selectedItem.getTitle());
                }
            } catch (IOException e) {
                MessageUtil.warningAlert(e);
                changeMediaLocation();
            }
        }
    }

    private void initMediaPlayer() {
        this.javaFxMediaPlayer = new JavaFxMediaPlayer();
        this.javaFxMediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Platform.runLater(MediaHandAppController.this::onMediaFinished);
            }
        });

        StackPane stackPane = this.javaFxMediaPlayer.getStackPane();
        this.mediaPlayerContextMenu = new MediaPlayerContextMenu(this.javaFxMediaPlayer.getEmbeddedMediaPlayer(), stackPane);
        this.controlPane = new ControlPane(this.javaFxMediaPlayer.getEmbeddedMediaPlayer(), this.jfxMediaHandApplication, this.javaFxMediaPlayer.getScene(), this.mediaEntryRepository);
        stackPane.getChildren().add(this.controlPane.getBorderPane());

        this.jfxMediaHandApplication.getStage().setOnCloseRequest(new StopRenderingSceneHandler(List.of(this.controlPane, this.javaFxMediaPlayer, this.mediaTeaser)));
        this.jfxMediaHandApplication.getStage().setFullScreenExitKeyCombination(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
    }

    /**
     * Play the selected media entry's current episode with the default program for that file.
     */
    private void playMedia() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            MessageUtil.infoAlert("Play media", "Please select a media entry.");
        } else if (!selectedItem.isAvailable()) {
            MessageUtil.infoAlert(
                    "Play media: " + selectedItem.getAbsolutePath(), "Selected media is not available. Deselect 'Show All' to show only media of connected media directories.");
        } else {
            File file = null;
            try {
                file = MediaLoader.getEpisode(selectedItem.getAbsolutePath(), selectedItem.getCurrentEpisode());
            } catch (IOException e) {
                MessageUtil.warningAlert(e);
                changeMediaLocation();
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.open(file);
                    this.mediaTeaser.pause();
                    this.mediaTeaser.switchImageView(false);
                } catch (IOException e) {
                    MessageUtil.warningAlert(e);
                }
            } else {
                try {
                    Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", file.getAbsolutePath()});
                    this.mediaTeaser.pause();
                    this.mediaTeaser.switchImageView(false);
                } catch (IOException e) {
                    MessageUtil.warningAlert(e);
                }
            }
        }
    }

    private void changeMediaLocation() {
        Optional<File> directory = this.jfxMediaHandApplication.chooseMediaDirectory();
        if (directory.isPresent()) {
            MediaEntry updatedMediaEntry = this.mediaLoader.createTempMediaEntry(directory.get().toPath());
            updateMedia(updatedMediaEntry);
        }
    }

    private void updateMedia(final MediaEntry mediaEntry) {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            this.mediaLoader.updateMediaEntry(mediaEntry, selectedItem);
        }
    }

    public void increaseCurrentEpisode() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisode() < selectedItem.getEpisodeNumber()) {
            selectedItem.setCurrentEpisode(selectedItem.getCurrentEpisode() + 1);
            this.mediaEntryRepository.save(selectedItem);
            MediaHandAppController.triggerMediaEntryUpdate(selectedItem);
        }
    }

    public void decreaseCurrentEpisode() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisode() > 1) {
            selectedItem.setCurrentEpisode(selectedItem.getCurrentEpisode() - 1);
            this.mediaEntryRepository.save(selectedItem);
            MediaHandAppController.triggerMediaEntryUpdate(selectedItem);
        }
    }

    public static void triggerMediaEntryUpdate(MediaEntry mediaEntry) {
        MediaHandAppController.mediaEntries.set(MediaHandAppController.mediaEntries.indexOf(mediaEntry), mediaEntry);
    }

    public static ObservableList<MediaEntry> getMediaEntries() {
        return MediaHandAppController.mediaEntries;
    }

    public MediaEntry getSelectedMediaEntry() {
        return this.mediaTableView.getSelectionModel().getSelectedItem();
    }

    public void onFilter() {
        MediaHandAppController.filteredData.setPredicate(this::filter);
    }

    private boolean filter(final MediaEntry mediaEntry) {
        return filter(mediaEntry, this.titleFilter.textProperty().getValue());
    }

    /**
     * Apply all filters with current selections and input. Return true, if the {@code mediaEntry} should be shown.
     *
     * @param mediaEntry the media entry to filter
     * @param textFilter the text filter to apply
     * @return true, if the media entry should be shown
     */
    private boolean filter(final MediaEntry mediaEntry, final String textFilter) {
        if ((this.showAllCheckbox.isSelected() || mediaEntry.isAvailable()) && mediaEntry.filterByWatchState(this.watchStateFilter.getSelectionModel().getSelectedItem())
                && mediaEntry.filterByMediaType(this.typeFilter.getSelectionModel().getSelectedItem())) {
            if (textFilter == null || textFilter.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = textFilter.toLowerCase();

            return mediaEntry.getTitle().toLowerCase().contains(lowerCaseFilter);
        }
        return false;
    }

    public void decreaseWatched() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getWatchedCount() > 0) {
            selectedItem.setWatchedCount(selectedItem.getWatchedCount() - 1);
            this.mediaEntryRepository.save(selectedItem);
            MediaHandAppController.triggerMediaEntryUpdate(selectedItem);
        }
    }

    public void increaseWatched() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            selectedItem.setWatchedCount(selectedItem.getWatchedCount() + 1);
            this.mediaEntryRepository.save(selectedItem);
            MediaHandAppController.triggerMediaEntryUpdate(selectedItem);
        }
    }

}
