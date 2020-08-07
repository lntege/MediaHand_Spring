package com.intege.mediahand.core;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.intege.mediahand.MediaLoader;
import com.intege.mediahand.WatchState;
import com.intege.mediahand.controller.MediaHandAppController;
import com.intege.mediahand.controller.RootLayoutController;
import com.intege.mediahand.domain.DirectoryEntry;
import com.intege.mediahand.domain.SettingsEntry;
import com.intege.mediahand.domain.repository.DirectoryEntryRepository;
import com.intege.mediahand.domain.repository.SettingsEntryRepository;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;

public class JfxMediaHandApplication extends Application {

    public static final String MEDIA_HAND_TITLE = "Media Hand";

    private ConfigurableApplicationContext applicationContext;

    private static Stage stage;
    private static MediaHandAppController mediaHandAppController;
    private static Scene scene;

    private BorderPane rootLayout;
    private SettingsEntry settingsEntry;

    @Autowired
    private DirectoryEntryRepository directoryEntryRepository;

    @Autowired
    private SettingsEntryRepository settingsEntryRepository;

    @Autowired
    private MediaLoader mediaLoader;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);

        this.applicationContext = new SpringApplicationBuilder().sources(MediaHandApplication.class).run(args);
        this.applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
    }

    @Override
    public void start(Stage stage) {
        JfxMediaHandApplication.stage = stage;
        this.mediaLoader = new MediaLoader();

        validateBasePath();

        initRootLayout();

        showMediaHand();

        applyFilterSettings();
    }

    @Override
    public void stop() {
        mediaHandAppController.stopControllerListener();
        int width = (int) stage.getWidth();
        int height = (int) stage.getHeight();
        this.settingsEntry.setWindowWidth(width);
        this.settingsEntry.setWindowHeight(height);
        this.settingsEntry.setWindowPositionX((int) stage.getX());
        this.settingsEntry.setWindowPositionY((int) stage.getY());
        this.settingsEntry.setAutoContinue(mediaHandAppController.autoContinueCheckbox.isSelected());
        this.settingsEntry.setShowAll(mediaHandAppController.showAllCheckbox.isSelected());
        this.settingsEntry.setWatchState(WatchState.lookupByName(mediaHandAppController.watchStateFilter.getSelectionModel().getSelectedItem()));
        this.settingsEntryRepository.save(this.settingsEntry);

        this.applicationContext.close();
        Platform.exit();
    }

    private void initRootLayout() {
        FxWeaver fxWeaver = this.applicationContext.getBean(FxWeaver.class);
        this.rootLayout = fxWeaver.loadView(RootLayoutController.class);

        scene = new Scene(this.rootLayout);
        setDefaultScene();

        this.settingsEntry = this.settingsEntryRepository.findByProfile("default");
        if (this.settingsEntry == null) {
            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            this.settingsEntry = this.settingsEntryRepository.save(new com.intege.mediahand.domain.SettingsEntry("default", 1200, 800, false, false, WatchState.ALL, (int) (
                    screenBounds.getWidth() / 2 - 600), (int) (screenBounds.getHeight() / 2 - 400)));
        }
        stage.setWidth(this.settingsEntry.getWindowWidth());
        stage.setHeight(this.settingsEntry.getWindowHeight());
        stage.setX(this.settingsEntry.getWindowPositionX());
        stage.setY(this.settingsEntry.getWindowPositionY());

        stage.show();
    }

    private void validateBasePath() {
        if (this.directoryEntryRepository.findAll().size() == 0) {
            addBasePath();
        }
    }

    private void showMediaHand() {
        FxWeaver fxWeaver = this.applicationContext.getBean(FxWeaver.class);
        FxControllerAndView<MediaHandAppController, Node> controllerAndView = fxWeaver.load(MediaHandAppController.class);
        assert controllerAndView.getView().isPresent();
        this.rootLayout.setCenter(controllerAndView.getView().get());
        mediaHandAppController = controllerAndView.getController();
        mediaHandAppController.init(); // TODO [lueko]: add scene as parameter and make scene non-static
    }

    private void applyFilterSettings() {
        mediaHandAppController.autoContinueCheckbox.setSelected(this.settingsEntry.isAutoContinue());
        mediaHandAppController.showAllCheckbox.setSelected(this.settingsEntry.isShowAll());
        mediaHandAppController.watchStateFilter.getSelectionModel().select(this.settingsEntry.getWatchStateValue());
        mediaHandAppController.onFilter();
    }

    public static void setDefaultScene() {
        stage.setScene(scene);
        stage.setTitle(MEDIA_HAND_TITLE);
        if (mediaHandAppController != null) {
            mediaHandAppController.startControllerListener();
        }
    }

    public boolean addBasePath() {
        Optional<File> baseDir = chooseMediaDirectory();

        if (baseDir.isPresent()) {
            this.mediaLoader.addMedia(this.directoryEntryRepository.save(new DirectoryEntry(baseDir.get().getAbsolutePath())));
            return true;
        }
        return false;
    }

    /**
     * Opens a dialog to choose a directory of the file system.
     *
     * @return the chosen directory
     */
    public static Optional<File> chooseMediaDirectory(final Path initialDirPath) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (initialDirPath != null) {
            directoryChooser.setInitialDirectory(initialDirPath.toFile());
        }
        File dialog = directoryChooser.showDialog(getStage());

        return Optional.ofNullable(dialog);
    }

    /**
     * Opens a dialog to choose a directory of the file system.
     *
     * @return the chosen directory
     */
    public static Optional<File> chooseMediaDirectory() {
        return chooseMediaDirectory(null);
    }

    public static MediaHandAppController getMediaHandAppController() {
        return mediaHandAppController;
    }

    public static Stage getStage() {
        return stage;
    }

    public static Scene getScene() {
        return scene;
    }
}
