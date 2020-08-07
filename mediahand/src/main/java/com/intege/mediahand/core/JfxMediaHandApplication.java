package com.intege.mediahand.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.intege.mediahand.MediaLoader;
import com.intege.mediahand.WatchState;
import com.intege.mediahand.controller.MediaHandAppController;
import com.intege.mediahand.controller.RootLayoutController;
import com.intege.mediahand.domain.SettingsEntry;
import com.intege.mediahand.domain.old.DirectoryEntry;
import com.intege.mediahand.domain.repository.SettingsEntryRepository;
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

public class JfxMediaHandApplication extends Application {

    public static final String MEDIA_HAND_TITLE = "Media Hand";

    private ConfigurableApplicationContext applicationContext;

    private static Stage stage;
    private static MediaLoader mediaLoader;
    private static MediaHandAppController mediaHandAppController;
    private static Scene scene;

    private BorderPane rootLayout;
    private SettingsEntry settingsEntry;

    private final SettingsEntryRepository settingsEntryRepository;

    @Autowired
    public JfxMediaHandApplication(final SettingsEntryRepository settingsEntryRepository) {
        this.settingsEntryRepository = settingsEntryRepository;
    }

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);

        this.applicationContext = new SpringApplicationBuilder().sources(MediaHandApplication.class).run(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        JfxMediaHandApplication.stage = stage;
        JfxMediaHandApplication.mediaLoader = new MediaLoader();

        validateBasePath();

        initRootLayout();

        showMediaHand();

        applyFilterSettings();
    }

    @Override
    @Transactional
    public void stop() {
        JfxMediaHandApplication.mediaHandAppController.stopControllerListener();
        int width = (int) JfxMediaHandApplication.stage.getWidth();
        int height = (int) JfxMediaHandApplication.stage.getHeight();
        this.settingsEntry.setWindowWidth(width);
        this.settingsEntry.setWindowHeight(height);
        this.settingsEntry.setAutoContinue(JfxMediaHandApplication.mediaHandAppController.autoContinueCheckbox.isSelected());
        this.settingsEntry.setShowAll(JfxMediaHandApplication.mediaHandAppController.showAllCheckbox.isSelected());
        this.settingsEntry.setWatchState(WatchState.lookupByName(JfxMediaHandApplication.mediaHandAppController.watchStateFilter.getSelectionModel().getSelectedItem()));
        //        RepositoryFactory.getSettingsRepository().update(this.settingsEntry);

        this.applicationContext.close();
        Platform.exit();
    }

    private void initRootLayout() throws IOException {
        FxWeaver fxWeaver = this.applicationContext.getBean(FxWeaver.class);
        this.rootLayout = fxWeaver.loadView(RootLayoutController.class);

        JfxMediaHandApplication.scene = new Scene(this.rootLayout);
        JfxMediaHandApplication.setDefaultScene();

        //        this.settingsEntry = RepositoryFactory.getSettingsRepository().create(new SettingsEntry("default", 1200, 800, false, false, null));
        this.settingsEntry = this.settingsEntryRepository.findByProfile("default");
        if (this.settingsEntry == null) {
            this.settingsEntry = this.settingsEntryRepository.save(new com.intege.mediahand.domain.SettingsEntry("default", 1200, 800, false, false, null));
        }
        JfxMediaHandApplication.stage.setWidth(this.settingsEntry.getWindowWidth());
        JfxMediaHandApplication.stage.setHeight(this.settingsEntry.getWindowHeight());

        JfxMediaHandApplication.stage.show();

        Runtime.getRuntime().addShutdownHook(new Thread(Database.getInstance()::closeConnections));
    }

    private void validateBasePath() {
        if (RepositoryFactory.getBasePathRepository().findAll().size() == 0) {
            JfxMediaHandApplication.addBasePath();
        }
    }

    private void showMediaHand() {
        FxWeaver fxWeaver = this.applicationContext.getBean(FxWeaver.class);
        FxControllerAndView<MediaHandAppController, Node> controllerAndView = fxWeaver.load(MediaHandAppController.class);
        assert controllerAndView.getView().isPresent();
        this.rootLayout.setCenter(controllerAndView.getView().get());
        JfxMediaHandApplication.mediaHandAppController = controllerAndView.getController();
        JfxMediaHandApplication.mediaHandAppController.init(); // TODO [lueko]: add scene as parameter and make scene non-static
    }

    private void applyFilterSettings() {
        JfxMediaHandApplication.mediaHandAppController.autoContinueCheckbox.setSelected(this.settingsEntry.isAutoContinue());
        JfxMediaHandApplication.mediaHandAppController.showAllCheckbox.setSelected(this.settingsEntry.isShowAll());
        JfxMediaHandApplication.mediaHandAppController.watchStateFilter.getSelectionModel().select(this.settingsEntry.getWatchStateValue());
        JfxMediaHandApplication.mediaHandAppController.onFilter();
    }

    public static void setDefaultScene() {
        JfxMediaHandApplication.stage.setScene(JfxMediaHandApplication.scene);
        JfxMediaHandApplication.stage.setTitle(JfxMediaHandApplication.MEDIA_HAND_TITLE);
        if (JfxMediaHandApplication.mediaHandAppController != null) {
            JfxMediaHandApplication.mediaHandAppController.startControllerListener();
        }
    }

    public static boolean addBasePath() {
        Optional<File> baseDir = JfxMediaHandApplication.chooseMediaDirectory();

        if (baseDir.isPresent()) {
            JfxMediaHandApplication.mediaLoader.addMedia(RepositoryFactory.getBasePathRepository().create(new DirectoryEntry(baseDir.get().getAbsolutePath())));
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
        File dialog = directoryChooser.showDialog(JfxMediaHandApplication.getStage());

        return Optional.ofNullable(dialog);
    }

    /**
     * Opens a dialog to choose a directory of the file system.
     *
     * @return the chosen directory
     */
    public static Optional<File> chooseMediaDirectory() {
        return JfxMediaHandApplication.chooseMediaDirectory(null);
    }

    public static MediaHandAppController getMediaHandAppController() {
        return JfxMediaHandApplication.mediaHandAppController;
    }

    public static MediaLoader getMediaLoader() {
        return JfxMediaHandApplication.mediaLoader;
    }

    public static Stage getStage() {
        return JfxMediaHandApplication.stage;
    }

    public static Scene getScene() {
        return JfxMediaHandApplication.scene;
    }
}
