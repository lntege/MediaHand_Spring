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
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;

@Slf4j
public class JfxMediaHandApplication extends Application {

    public static final String MEDIA_HAND_TITLE = "Media Hand";

    private ConfigurableApplicationContext applicationContext;

    private Stage stage;

    private Scene scene;

    private BorderPane rootLayout;

    private SettingsEntry settingsEntry;

    private MediaHandAppController mediaHandAppController;

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
        this.applicationContext.getBeanFactory().registerSingleton("jfxMediaHandApplication", this);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Image icon = new Image("/video.png");
        this.stage.getIcons().add(icon);
        this.mediaLoader = new MediaLoader();

        validateBasePath();

        initRootLayout();

        showMediaHand();

        applyFilterSettings();
    }

    @Override
    public void stop() {
        this.mediaHandAppController.stopControllerListener();
        int width = (int) this.stage.getWidth();
        int height = (int) this.stage.getHeight();
        this.settingsEntry.setWindowWidth(width);
        this.settingsEntry.setWindowHeight(height);
        this.settingsEntry.setWindowPositionX((int) this.stage.getX());
        this.settingsEntry.setWindowPositionY((int) this.stage.getY());
        this.settingsEntry.setAutoContinue(this.mediaHandAppController.autoContinueCheckbox.isSelected());
        this.settingsEntry.setShowAll(this.mediaHandAppController.showAllCheckbox.isSelected());
        this.settingsEntry.setWatchState(WatchState.lookupByName(this.mediaHandAppController.watchStateFilter.getSelectionModel().getSelectedItem()));
        this.settingsEntry.setPlayTeaser(this.mediaHandAppController.playTeaser.isSelected());
        this.settingsEntryRepository.save(this.settingsEntry);

        try {
            if (this.mediaHandAppController.getCheckAllThumbnailsThread() != null) {
                this.mediaHandAppController.getCheckAllThumbnailsThread().join();
            }
            if (this.mediaHandAppController.getCheckThumbnailOnRequestThread() != null) {
                this.mediaHandAppController.getCheckThumbnailOnRequestThread().join();
            }
        } catch (InterruptedException e) {
            log.error("Could not wait for check thumbnail thread to finish", e);
        }

        this.applicationContext.close();
        Platform.exit();
    }

    private void initRootLayout() {
        FxWeaver fxWeaver = this.applicationContext.getBean(FxWeaver.class);
        this.rootLayout = fxWeaver.loadView(RootLayoutController.class);

        this.scene = new Scene(this.rootLayout);
        setDefaultScene();

        this.settingsEntry = this.settingsEntryRepository.findByProfile("default");
        if (this.settingsEntry == null) {
            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            this.settingsEntry = this.settingsEntryRepository.save(new SettingsEntry("default", 1200, 800, false, false, WatchState.ALL, (int) (screenBounds.getWidth() / 2 - 600), (int) (screenBounds.getHeight() / 2 - 400), false));
        }
        this.stage.setWidth(this.settingsEntry.getWindowWidth());
        this.stage.setHeight(this.settingsEntry.getWindowHeight());
        this.stage.setX(this.settingsEntry.getWindowPositionX());
        this.stage.setY(this.settingsEntry.getWindowPositionY());

        this.stage.show();
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
        this.mediaHandAppController = controllerAndView.getController();
        this.mediaHandAppController.init();
        this.mediaHandAppController.playTeaser.setSelected(this.settingsEntry.isPlayTeaser());
    }

    private void applyFilterSettings() {
        this.mediaHandAppController.autoContinueCheckbox.setSelected(this.settingsEntry.isAutoContinue());
        this.mediaHandAppController.showAllCheckbox.setSelected(this.settingsEntry.isShowAll());
        this.mediaHandAppController.watchStateFilter.getSelectionModel().select(this.settingsEntry.getWatchStateValue());
        this.mediaHandAppController.onFilter();
    }

    public void setDefaultScene() {
        this.stage.setScene(this.scene);
        this.stage.setTitle(MEDIA_HAND_TITLE);
        if (this.mediaHandAppController != null) {
            this.mediaHandAppController.startControllerListener();
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
    public Optional<File> chooseMediaDirectory(final Path initialDirPath) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (initialDirPath != null) {
            directoryChooser.setInitialDirectory(initialDirPath.toFile());
        }
        File dialog = directoryChooser.showDialog(this.stage);

        return Optional.ofNullable(dialog);
    }

    /**
     * Opens a dialog to choose a directory of the file system.
     *
     * @return the chosen directory
     */
    public Optional<File> chooseMediaDirectory() {
        return chooseMediaDirectory(null);
    }

    public MediaHandAppController getMediaHandAppController() {
        return this.mediaHandAppController;
    }

    public Stage getStage() {
        return this.stage;
    }

}
