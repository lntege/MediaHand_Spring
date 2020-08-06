package com.intege.mediahand.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.intege.mediahand.MediaLoader;
import com.intege.mediahand.WatchState;
import com.intege.mediahand.controller.MediaHandAppController;
import com.intege.mediahand.controller.RootLayoutController;
import com.intege.mediahand.domain.old.DirectoryEntry;
import com.intege.mediahand.domain.old.SettingsEntry;
import com.intege.mediahand.repository.RepositoryFactory;
import com.intege.mediahand.repository.base.Database;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;

public class JFxMediaHandApplication extends Application {

    public static final String MEDIA_HAND_TITLE = "Media Hand";

    private ConfigurableApplicationContext applicationContext;

    private static Stage stage;
    private static MediaLoader mediaLoader;
    private static MediaHandAppController mediaHandAppController;
    private static Scene scene;

    private BorderPane rootLayout;
    private SettingsEntry settingsEntry;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);

        this.applicationContext = new SpringApplicationBuilder()
                .sources(MediaHandApplication.class)
                .run(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        JFxMediaHandApplication.stage = stage;
        JFxMediaHandApplication.mediaLoader = new MediaLoader();

        validateBasePath();

        initRootLayout();

        showMediaHand();

        applyFilterSettings();
    }

    @Override
    public void stop() {
        JFxMediaHandApplication.mediaHandAppController.stopControllerListener();
        int width = (int) JFxMediaHandApplication.stage.getWidth();
        int height = (int) JFxMediaHandApplication.stage.getHeight();
        this.settingsEntry.setWindowWidth(width);
        this.settingsEntry.setWindowHeight(height);
        this.settingsEntry.setAutoContinue(JFxMediaHandApplication.mediaHandAppController.autoContinueCheckbox.isSelected());
        this.settingsEntry.setShowAll(JFxMediaHandApplication.mediaHandAppController.showAllCheckbox.isSelected());
        this.settingsEntry.setWatchState(WatchState.lookupByName(JFxMediaHandApplication.mediaHandAppController.watchStateFilter.getSelectionModel().getSelectedItem()));
        RepositoryFactory.getSettingsRepository().update(this.settingsEntry);

        this.applicationContext.close();
        Platform.exit();
    }

    private void initRootLayout() throws IOException {
        FxWeaver fxWeaver = this.applicationContext.getBean(FxWeaver.class);
        this.rootLayout = fxWeaver.loadView(RootLayoutController.class);

        JFxMediaHandApplication.scene = new Scene(this.rootLayout);
        JFxMediaHandApplication.setDefaultScene();

        this.settingsEntry = RepositoryFactory.getSettingsRepository().create(new SettingsEntry("default", 1200, 800, false, false, null));
        JFxMediaHandApplication.stage.setWidth(this.settingsEntry.getWindowWidth());
        JFxMediaHandApplication.stage.setHeight(this.settingsEntry.getWindowHeight());

        JFxMediaHandApplication.stage.show();

        Runtime.getRuntime().addShutdownHook(new Thread(Database.getInstance()::closeConnections));
    }

    private void validateBasePath() {
        if (RepositoryFactory.getBasePathRepository().findAll().size() == 0) {
            JFxMediaHandApplication.addBasePath();
        }
    }

    private void showMediaHand() {
        FxWeaver fxWeaver = this.applicationContext.getBean(FxWeaver.class);
        FxControllerAndView<MediaHandAppController, Node> controllerAndView = fxWeaver.load(MediaHandAppController.class);
        this.rootLayout.setCenter(controllerAndView.getView().get());
        JFxMediaHandApplication.mediaHandAppController = controllerAndView.getController();
        JFxMediaHandApplication.mediaHandAppController.init(); // TODO [lueko]: add scene as parameter and make scene non-static
    }

    private void applyFilterSettings() {
        JFxMediaHandApplication.mediaHandAppController.autoContinueCheckbox.setSelected(this.settingsEntry.isAutoContinue());
        JFxMediaHandApplication.mediaHandAppController.showAllCheckbox.setSelected(this.settingsEntry.isShowAll());
        JFxMediaHandApplication.mediaHandAppController.watchStateFilter.getSelectionModel().select(this.settingsEntry.getWatchStateValue());
        JFxMediaHandApplication.mediaHandAppController.onFilter();
    }

    public static void setDefaultScene() {
        JFxMediaHandApplication.stage.setScene(JFxMediaHandApplication.scene);
        JFxMediaHandApplication.stage.setTitle(JFxMediaHandApplication.MEDIA_HAND_TITLE);
        if (JFxMediaHandApplication.mediaHandAppController != null) {
            JFxMediaHandApplication.mediaHandAppController.startControllerListener();
        }
    }

    public static boolean addBasePath() {
        Optional<File> baseDir = JFxMediaHandApplication.chooseMediaDirectory();

        if (baseDir.isPresent()) {
            JFxMediaHandApplication.mediaLoader.addMedia(RepositoryFactory.getBasePathRepository().create(new DirectoryEntry(baseDir.get().getAbsolutePath())));
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
        File dialog = directoryChooser.showDialog(JFxMediaHandApplication.getStage());

        return Optional.ofNullable(dialog);
    }

    /**
     * Opens a dialog to choose a directory of the file system.
     *
     * @return the chosen directory
     */
    public static Optional<File> chooseMediaDirectory() {
        return JFxMediaHandApplication.chooseMediaDirectory(null);
    }

    public static MediaHandAppController getMediaHandAppController() {
        return JFxMediaHandApplication.mediaHandAppController;
    }

    public static MediaLoader getMediaLoader() {
        return JFxMediaHandApplication.mediaLoader;
    }

    public static Stage getStage() {
        return JFxMediaHandApplication.stage;
    }

    public static Scene getScene() {
        return JFxMediaHandApplication.scene;
    }
}
