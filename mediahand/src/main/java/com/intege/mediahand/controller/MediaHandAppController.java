package com.intege.mediahand.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.intege.mediahand.WatchState;
import com.intege.mediahand.core.JFxMediaHandApplication;
import com.intege.mediahand.domain.old.MediaEntry;
import com.intege.mediahand.repository.RepositoryFactory;
import com.intege.mediahand.utils.MessageUtil;
import com.intege.mediahand.vlc.ControlPane;
import com.intege.mediahand.vlc.JavaFxMediaPlayer;
import com.intege.mediahand.vlc.MediaPlayerContextMenu;
import com.intege.mediahand.vlc.event.StopRenderingSceneHandler;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerUnpluggedException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import net.rgielen.fxweaver.core.FxmlView;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

@Component
@FxmlView("mediaHandApp.fxml")
public class MediaHandAppController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaHandAppController.class);

    public TableView<MediaEntry> mediaTableView;

    private static ObservableList<MediaEntry> mediaEntries;
    private static FilteredList<MediaEntry> filteredData;

    public TextField titleFilter;
    public ComboBox<String> watchStateFilter;
    public ComboBox<String> typeFilter;
    public CheckBox showAllCheckbox;
    public CheckBox autoContinueCheckbox;

    public ComboBox<String> watchStateEdit;
    public ComboBox<Integer> ratingEdit;
    public DatePicker watchedEdit;
    public ComboBox<Integer> episodeEdit;
    public Label selectedMediaTitle;

    public TableColumn<MediaEntry, String> title;

    private ControllerIndex currentController;

    private boolean isRunning;

    private JavaFxMediaPlayer javaFxMediaPlayer;
    private ControlPane controlPane;
    private MediaPlayerContextMenu mediaPlayerContextMenu;

    public void init() {
        initMediaPlayer();

        startControllerListener();
        addWatchStateFilter();
        addMediaTypeFilter();
        addTitleFieldFilterListener();
        this.title.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                if (item != null) {
                    setTooltip(new Tooltip(RepositoryFactory.getMediaRepository().find(new MediaEntry(item)).getAbsolutePath()));
                }
            }
        });
        this.watchStateEdit.setItems(FXCollections.observableArrayList(WatchState.WANT_TO_WATCH.toString(), WatchState.DOWNLOADING.toString(), WatchState.WATCHED.toString(), WatchState.WATCHING.toString(), WatchState.REWATCHING.toString()));
        this.ratingEdit.setItems(FXCollections.observableArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
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
                this.episodeEdit.getSelectionModel().select(newValue.getCurrentEpisodeNumber() - 1);
                this.watchedEdit.setValue(newValue.getWatchedDate());
            } else {
                this.selectedMediaTitle.setText("Selected media");
                this.selectedMediaTitle.setTooltip(new Tooltip("Selected media"));
            }
        });
        this.ratingEdit.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && newValue != null && selectedItem.getRating() != newValue) {
                selectedItem.setRating(newValue);
                RepositoryFactory.getMediaRepository().update(selectedItem);
            }
        });
        this.episodeEdit.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && newValue != null && selectedItem.getCurrentEpisodeNumber() != newValue) {
                selectedItem.setCurrentEpisodeNumber(newValue);
                RepositoryFactory.getMediaRepository().update(selectedItem);
            }
        });
        this.watchStateEdit.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !Objects.equals(selectedItem.getWatchState().toString(), newValue)) {
                selectedItem.setWatchState(WatchState.lookupByName(newValue));
                RepositoryFactory.getMediaRepository().update(selectedItem);
            }
        });
        this.watchedEdit.valueProperty().addListener((observable, oldValue, newValue) -> {
            MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !Objects.equals(selectedItem.getWatchedDate(), newValue)) {
                selectedItem.setWatchedDate(newValue);
                RepositoryFactory.getMediaRepository().update(selectedItem);
            }
        });
        fillTableView(RepositoryFactory.getMediaRepository().findAll());
    }

    public void onMediaFinished() {
        this.controlPane.stop();
        boolean fullScreen = JFxMediaHandApplication.getStage().isFullScreen();
        increaseCurrentEpisode();
        if (this.autoContinueCheckbox.isSelected()) {
            playEmbeddedMedia();
            JFxMediaHandApplication.getStage().setFullScreen(fullScreen);
        } else {
            JFxMediaHandApplication.setDefaultScene();
        }
    }

    public void startControllerListener() {
        if (this.isRunning) {
            return;
        }
        ControllerManager controllerManager = new ControllerManager();
        controllerManager.initSDLGamepad();
        this.currentController = controllerManager.getControllerIndex(0);
        Thread thread = new Thread(() -> {
            this.isRunning = true;
            while (this.isRunning) {
                controllerManager.update();
                try {
                    if (this.currentController.isButtonJustPressed(ControllerButton.DPAD_DOWN) || (
                            this.currentController.isButtonPressed(ControllerButton.DPAD_DOWN)
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
                    if (this.currentController.isButtonJustPressed(ControllerButton.DPAD_UP) || (
                            this.currentController.isButtonPressed(ControllerButton.DPAD_UP)
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
                    MediaHandAppController.LOGGER.error("Controller thread: sleep", e);
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
        this.watchStateFilter.setItems(FXCollections.observableArrayList("ALL", WatchState.WANT_TO_WATCH.toString(), WatchState.DOWNLOADING.toString(), WatchState.WATCHED.toString(), WatchState.WATCHING.toString(), WatchState.REWATCHING.toString()));
        this.watchStateFilter.getSelectionModel().select("ALL");
    }

    private void addMediaTypeFilter() {
        List<String> mediaTypes = new ArrayList<>(RepositoryFactory.getMediaRepository().findAllMediaTypes());
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

    public void playEmbeddedMedia() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            MessageUtil.infoAlert("Play media", "Please select a media entry.");
        } else if (!selectedItem.isAvailable()) {
            MessageUtil.infoAlert("Play media: "
                    + selectedItem.getAbsolutePath(), "Selected media is not available. Deselect 'Show All' to show only media of connected media directories.");
        } else {
            try {
                File file = JFxMediaHandApplication.getMediaLoader().getEpisode(selectedItem.getAbsolutePath(), selectedItem.getCurrentEpisodeNumber());

                String windowTitle = selectedItem.getTitle() + " : Episode " + selectedItem.getCurrentEpisodeNumber();
                this.isRunning = false;

                JFxMediaHandApplication.getStage().setTitle(windowTitle);
                JFxMediaHandApplication.getStage().setScene(this.javaFxMediaPlayer.getScene());

                if (this.javaFxMediaPlayer.start(file)) {
                    this.controlPane.update(selectedItem);
                    this.mediaPlayerContextMenu.update(selectedItem);
                } else {
                    MessageUtil.warningAlert("Play embedded media failed",
                            "Could not play selected entry " + selectedItem.getTitle());
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
        this.controlPane = new ControlPane(this.javaFxMediaPlayer.getEmbeddedMediaPlayer(), this.javaFxMediaPlayer.getScene());
        stackPane.getChildren().add(this.controlPane.getBorderPane());

        JFxMediaHandApplication.getStage().setOnCloseRequest(new StopRenderingSceneHandler(List.of(this.controlPane, this.javaFxMediaPlayer)));
        JFxMediaHandApplication.getStage().setFullScreenExitKeyCombination(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
    }

    private void playMedia() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            MessageUtil.infoAlert("Play media", "Please select a media entry.");
        } else if (!selectedItem.isAvailable()) {
            MessageUtil.infoAlert("Play media: "
                    + selectedItem.getAbsolutePath(), "Selected media is not available. Deselect 'Show All' to show only media of connected media directories.");
        } else {
            Desktop desktop = Desktop.getDesktop();
            try {
                File file = JFxMediaHandApplication.getMediaLoader().getEpisode(selectedItem.getAbsolutePath(), selectedItem.getCurrentEpisodeNumber());
                try {
                    desktop.open(file);
                } catch (IOException e) {
                    MessageUtil.warningAlert(e);
                }
            } catch (IOException e) {
                MessageUtil.warningAlert(e);
                changeMediaLocation();
            }
        }
    }

    private void changeMediaLocation() {
        Optional<File> directory = JFxMediaHandApplication.chooseMediaDirectory();
        if (directory.isPresent()) {
            MediaEntry updatedMediaEntry = JFxMediaHandApplication.getMediaLoader().createTempMediaEntry(directory.get().toPath());
            updateMedia(updatedMediaEntry);
        }
    }

    private void updateMedia(final MediaEntry mediaEntry) {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            JFxMediaHandApplication.getMediaLoader().updateMediaEntry(mediaEntry, RepositoryFactory.getMediaRepository(), selectedItem);
        }
    }

    public void increaseCurrentEpisode() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisodeNumber() < selectedItem.getEpisodeNumber()) {
            selectedItem.setCurrentEpisodeNumber(selectedItem.getCurrentEpisodeNumber() + 1);
            RepositoryFactory.getMediaRepository().update(selectedItem);
        }
    }

    public void decreaseCurrentEpisode() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getCurrentEpisodeNumber() > 1) {
            selectedItem.setCurrentEpisodeNumber(selectedItem.getCurrentEpisodeNumber() - 1);
            RepositoryFactory.getMediaRepository().update(selectedItem);
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

    private boolean filter(final MediaEntry mediaEntry, final String textFilter) {
        if ((this.showAllCheckbox.isSelected() || mediaEntry.isAvailable())
                && mediaEntry.filterByWatchState(this.watchStateFilter.getSelectionModel().getSelectedItem())
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
            RepositoryFactory.getMediaRepository().update(selectedItem);
        }
    }

    public void increaseWatched() {
        MediaEntry selectedItem = this.mediaTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            selectedItem.setWatchedCount(selectedItem.getWatchedCount() + 1);
            RepositoryFactory.getMediaRepository().update(selectedItem);
        }
    }
}
