package com.intege.mediahand.vlc.customRenderer;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intege.mediahand.core.JFxMediaHandApplication;
import com.intege.mediahand.domain.old.MediaEntry;
import com.intege.mediahand.repository.RepositoryFactory;
import com.intege.mediahand.vlc.MediaPlayerComponent;
import com.intege.mediahand.vlc.event.StopRenderingSceneHandler;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerUnpluggedException;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.TrackDescription;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

public class JavaFXDirectRenderingScene implements MediaPlayerComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaFXDirectRenderingScene.class);

    private static final double FPS = 60.0;

    private static final long DELAY = 1000;

    private final NanoTimer nanoTimer = new NanoTimer(1000.0 / JavaFXDirectRenderingScene.FPS) {
        @Override
        protected void onSucceeded() {
            renderFrame();
        }

        private void renderFrame() {
            GraphicsContext g = JavaFXDirectRenderingScene.this.canvas.getGraphicsContext2D();

            double width = JavaFXDirectRenderingScene.this.canvas.getWidth();
            double height = JavaFXDirectRenderingScene.this.canvas.getHeight();

            g.setFill(new Color(0, 0, 0, 1));
            g.fillRect(0, 0, width, height);

            if (JavaFXDirectRenderingScene.this.img != null) {
                double imageWidth = JavaFXDirectRenderingScene.this.img.getWidth();
                double imageHeight = JavaFXDirectRenderingScene.this.img.getHeight();

                double sx = width / imageWidth;
                double sy = height / imageHeight;

                double sf = Math.min(sx, sy);

                double scaledW = imageWidth * sf;
                double scaledH = imageHeight * sf;

                Affine ax = g.getTransform();

                g.translate(
                        (width - scaledW) / 2,
                        (height - scaledH) / 2
                );

                if (sf != 1.0) {
                    g.scale(sf, sf);
                }

                try {
                    JavaFXDirectRenderingScene.this.semaphore.acquire();
                    g.drawImage(JavaFXDirectRenderingScene.this.img, 0, 0);
                    JavaFXDirectRenderingScene.this.semaphore.release();
                } catch (InterruptedException e) {
                    JavaFXDirectRenderingScene.LOGGER.error("semaphore acquire", e);
                    Thread.currentThread().interrupt();
                }

                g.setTransform(ax);
            }
        }
    };

    /**
     * Filename of the video to play.
     */
    private final String videoFile;

    /**
     * Lightweight JavaFX canvas, the video is rendered here.
     */
    private final Canvas canvas;

    private Timer timer = new Timer();

    private ContextMenu contextMenu;

    /**
     * Pixel writer to update the canvas.
     */
    private PixelWriter pixelWriter;

    /**
     * Pixel format.
     */
    private final WritablePixelFormat<ByteBuffer> pixelFormat;

    private final StackPane stackPane;

    private final MediaPlayerFactory mediaPlayerFactory;

    /**
     * The vlcj direct rendering media player component.
     */
    private EmbeddedMediaPlayer mediaPlayer;

    private Stage stage;

    private WritableImage img;

    private Slider mediaTimeSlider;

    private final BorderPane controlPane;

    private final MediaEntry mediaEntry;

    private final ControllerIndex currentController;

    private boolean isRunning;

    public JavaFXDirectRenderingScene(final File videoFile, final MediaEntry mediaEntry) {
        ControllerManager controllerManager = new ControllerManager();
        controllerManager.initSDLGamepad();
        this.currentController = controllerManager.getControllerIndex(0);
        Thread thread = new Thread(() -> {
            this.isRunning = true;
            while (this.isRunning) {
                controllerManager.update();
                try {
                    if (this.currentController.isButtonJustPressed(ControllerButton.B)) {
                        this.mediaPlayer.controls().pause();
                        TimerTask timerTask = showTimeSlider();
                        this.timer = new Timer();
                        this.timer.schedule(timerTask, JavaFXDirectRenderingScene.DELAY * 3);
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.RIGHTBUMPER)) {
                        Platform.runLater(this::playNextEpisode);
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.LEFTBUMPER)) {
                        Platform.runLater(this::playPreviousEpisode);
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.X)) {
                        Platform.runLater(() -> JavaFXDirectRenderingScene.this.mediaPlayer.controls().skipTime(80000));
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.BACK)) {
                        Platform.runLater(() -> {
                            stop();
                            JFxMediaHandApplication.setDefaultScene();
                        });
                    }
                    if (this.currentController.isButtonPressed(ControllerButton.DPAD_LEFT)) {
                        showTimedTimeSlider(JavaFXDirectRenderingScene.DELAY);
                        if (this.currentController.isButtonPressed(ControllerButton.A)) {
                            Platform.runLater(() -> this.mediaPlayer.controls().skipTime(-3000));
                        } else {
                            Platform.runLater(() -> this.mediaPlayer.controls().skipTime(-1000));
                        }
                    }
                    if (this.currentController.isButtonPressed(ControllerButton.DPAD_RIGHT)) {
                        showTimedTimeSlider(JavaFXDirectRenderingScene.DELAY);
                        if (this.currentController.isButtonPressed(ControllerButton.A)) {
                            Platform.runLater(() -> this.mediaPlayer.controls().skipTime(3000));
                        } else {
                            Platform.runLater(() -> this.mediaPlayer.controls().skipTime(1000));
                        }
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.Y)) {
                        Platform.runLater(() -> this.stage.setFullScreen(!this.stage.isFullScreen()));
                    }
                } catch (ControllerUnpluggedException e) {
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    JavaFXDirectRenderingScene.LOGGER.error("Controller thread: sleep", e);
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();

        this.videoFile = videoFile.getAbsolutePath();
        this.mediaEntry = mediaEntry;

        this.canvas = new Canvas();

        this.pixelWriter = this.canvas.getGraphicsContext2D().getPixelWriter();
        this.pixelFormat = PixelFormat.getByteBgraInstance();

        this.mediaPlayerFactory = new MediaPlayerFactory();
        this.mediaPlayer = this.mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

        this.mediaPlayer.videoSurface().set(new JavaFxVideoSurface());

        this.stackPane = new StackPane();
        this.controlPane = new BorderPane();

        this.stackPane.getChildren().add(0, this.canvas);
        this.stackPane.getChildren().add(1, this.controlPane);
        this.stackPane.setStyle("-fx-background-color: rgb(0, 0, 0);");

        this.canvas.widthProperty().bind(this.stackPane.widthProperty());
        this.canvas.heightProperty().bind(this.stackPane.heightProperty());

        addContextMenuListeners();
    }

    public void start(final Stage primaryStage, final String title) {
        initStage(primaryStage, title);

        this.mediaPlayer.controls().setRepeat(false);

        this.mediaPlayer.media().play(this.videoFile);

        if (startTimer()) {
            onMediaLoaded();
        }
    }

    private void initStage(Stage primaryStage, String title) {
        this.stage = primaryStage;

        this.stage.setOnCloseRequest(new StopRenderingSceneHandler(List.of(this)));

        this.stage.setTitle(title);

        this.stage.setFullScreenExitKeyCombination(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));

        Scene scene = initScene();
        this.stage.setScene(scene);
        this.stage.show();
    }

    private Scene initScene() {
        Scene scene = new Scene(this.stackPane, Color.BLACK);
        addSceneKeyListeners(scene);
        return scene;
    }

    public void stop() {
        this.isRunning = false;
        this.stage.setOnCloseRequest(null);
        stopTimer();
        this.timer.cancel();

        RepositoryFactory.getMediaRepository().update(this.mediaEntry);

        this.mediaPlayer.controls().stop();
        this.mediaPlayer.release();
        this.mediaPlayerFactory.release();
    }

    private void onMediaFinished() {
        boolean fullScreen = JavaFXDirectRenderingScene.this.stage.isFullScreen();
        stop();
        JFxMediaHandApplication.getMediaHandAppController().increaseCurrentEpisode();
        if (JFxMediaHandApplication.getMediaHandAppController().autoContinueCheckbox.isSelected()) {
            playSelectedMedia(fullScreen);
        } else {
            JFxMediaHandApplication.setDefaultScene();
        }
    }

    private void playSelectedMedia(boolean fullScreen) {
        JFxMediaHandApplication.getMediaHandAppController().playEmbeddedMedia();
        JavaFXDirectRenderingScene.this.stage.setFullScreen(fullScreen);
    }

    private void onMediaLoaded() {
        buildContextMenu(this.mediaPlayer);

        setMediaPlayerEventListener();

        double mediaDuration = this.mediaPlayer.media().info().duration() / 60000.0;
        BorderPane sliderPane = initSliderPane(mediaDuration);
        showTimedTimeSlider(JavaFXDirectRenderingScene.DELAY * 3);

        this.controlPane.setBottom(sliderPane);
    }

    private void setMediaPlayerEventListener() {
        this.mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                updateMediaTimeSlider(newTime);
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                Platform.runLater(JavaFXDirectRenderingScene.this::onMediaFinished);
            }
        });
    }

    private void updateMediaTimeSlider(long newTime) {
        if (!JavaFXDirectRenderingScene.this.mediaTimeSlider.isPressed()) {
            JavaFXDirectRenderingScene.this.mediaTimeSlider.setValue(newTime / 60000.0);
        }
    }

    private BorderPane initSliderPane(final double mediaDuration) {
        this.mediaTimeSlider = initMediaTimeSlider(mediaDuration, this.mediaPlayer);
        Slider volumeSlider = initVolumeSlider(this.mediaPlayer, this.mediaEntry.getVolume());

        BorderPane sliderPane = new BorderPane(this.mediaTimeSlider);
        sliderPane.setRight(volumeSlider);

        addControlPaneMouseListener(sliderPane);
        return sliderPane;
    }

    private void addControlPaneMouseListener(BorderPane sliderPane) {
        this.stackPane.setOnMouseMoved(event -> {
            showTimedTimeSlider(JavaFXDirectRenderingScene.DELAY);
            TimerTask timerTask = showTimeSlider();
            if (sliderPane.sceneToLocal(event.getSceneX(), event.getSceneY()).getY() < -10) {
                this.timer = new Timer();
                this.timer.schedule(timerTask, JavaFXDirectRenderingScene.DELAY);
            }
        });
    }

    private void showTimedTimeSlider(long delay) {
        TimerTask timerTask = showTimeSlider();
        this.timer = new Timer();
        this.timer.schedule(timerTask, delay);
    }

    private TimerTask showTimeSlider() {
        this.controlPane.setVisible(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                JavaFXDirectRenderingScene.this.controlPane.setVisible(false);
            }
        };
        this.timer.cancel();
        return task;
    }

    private Slider initMediaTimeSlider(final double mediaDuration, final EmbeddedMediaPlayer mediaPlayer) {
        Slider slider = new Slider(0, mediaDuration, 0);
        slider.setMajorTickUnit(mediaDuration);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setOnMouseClicked(event -> mediaPlayer.controls().setTime((long) (slider.getValue() * 60000)));
        return slider;
    }

    private Slider initVolumeSlider(final EmbeddedMediaPlayer mediaPlayer, final int volume) {
        Slider volumeSlider = new Slider(0, 100, volume);
        mediaPlayer.audio().setVolume((int) volumeSlider.getValue());
        volumeSlider.setOnMouseClicked(event -> {
            int newVolume = (int) volumeSlider.getValue();
            mediaPlayer.audio().setVolume(newVolume);
            this.mediaEntry.setVolume(newVolume);
        });
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> mediaPlayer.audio().setVolume(newValue.intValue()));
        return volumeSlider;
    }

    private void addSceneKeyListeners(final Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stop();
                JFxMediaHandApplication.setDefaultScene();
            } else if (event.getCode() == KeyCode.SPACE) {
                this.mediaPlayer.controls().pause();
                showTimedTimeSlider(JavaFXDirectRenderingScene.DELAY * 3);
            } else if (event.getCode() == KeyCode.ENTER) {
                this.mediaPlayer.controls().skipTime(80000);
            } else if (!event.isControlDown() && event.getCode() == KeyCode.F) {
                this.stage.setFullScreen(true);
            } else if (event.getCode() == KeyCode.UP) {
                playNextEpisode();
            } else if (event.getCode() == KeyCode.DOWN) {
                playPreviousEpisode();
            }
        });
    }

    private void playNextEpisode() {
        boolean fullScreen = this.stage.isFullScreen();
        stop();
        JFxMediaHandApplication.getMediaHandAppController().increaseCurrentEpisode();
        playSelectedMedia(fullScreen);
    }

    private void playPreviousEpisode() {
        boolean fullScreen = this.stage.isFullScreen();
        stop();
        JFxMediaHandApplication.getMediaHandAppController().decreaseCurrentEpisode();
        playSelectedMedia(fullScreen);
    }

    private void addContextMenuListeners() {
        addShowContextMenuListener();
        addHideContextMenuListener();
    }

    private void addShowContextMenuListener() {
        this.stackPane.setOnContextMenuRequested(event -> this.contextMenu.show(this.stackPane, event.getScreenX(), event.getScreenY()));
    }

    private void addHideContextMenuListener() {
        this.stackPane.setOnMouseClicked(event -> {
            if (this.contextMenu != null) {
                this.contextMenu.hide();
            }
        });
    }

    private void buildContextMenu(final EmbeddedMediaPlayer mediaPlayer) {
        Menu audioMenu = buildAudioContextMenu(mediaPlayer);

        Menu subtitleMenu = buildSubtitleContextMenu(mediaPlayer);

        this.contextMenu = new ContextMenu(audioMenu, subtitleMenu);

        if (this.mediaEntry.getSubtitleTrack() != null) {
            Optional<MenuItem> menuItem = subtitleMenu.getItems().stream().filter(item -> item.getId().equals(this.mediaEntry.getSubtitleTrack())).findFirst();
            menuItem.ifPresent(item -> setSubtitleTrack(mediaPlayer, Integer.parseInt(this.mediaEntry.getSubtitleTrack()), item));
        }
        if (this.mediaEntry.getAudioTrack() != null) {
            Optional<MenuItem> menuItem = audioMenu.getItems().stream().filter(item -> item.getId().equals(this.mediaEntry.getAudioTrack())).findFirst();
            menuItem.ifPresent(item -> setAudioTrack(mediaPlayer, Integer.parseInt(this.mediaEntry.getAudioTrack()), item));
        }
    }

    private Menu buildSubtitleContextMenu(EmbeddedMediaPlayer mediaPlayer) {
        Menu subtitleMenu = new Menu("Subtitle");
        for (TrackDescription trackDescription : this.mediaPlayer.subpictures().trackDescriptions()) {
            MenuItem item = initSubtitleMenuItem(mediaPlayer, trackDescription);
            subtitleMenu.getItems().add(item);
        }
        return subtitleMenu;
    }

    private MenuItem initSubtitleMenuItem(EmbeddedMediaPlayer mediaPlayer, TrackDescription trackDescription) {
        MenuItem item = new MenuItem(trackDescription.description());
        item.setId(trackDescription.id() + "");
        if (trackDescription.id() == mediaPlayer.subpictures().track()) {
            highlightMenuItem(item);
        }
        item.setOnAction(event -> setSubtitleTrack(mediaPlayer, trackDescription.id(), item));
        return item;
    }

    private void setSubtitleTrack(EmbeddedMediaPlayer mediaPlayer, int trackId, MenuItem item) {
        resetStyleOfCurrentSubtitleTrack(mediaPlayer);
        mediaPlayer.subpictures().setTrack(trackId);
        highlightMenuItem(item);
        this.mediaEntry.setSubtitleTrack(trackId + "");
    }

    private Menu buildAudioContextMenu(EmbeddedMediaPlayer mediaPlayer) {
        Menu audioMenu = new Menu("Audio");
        for (TrackDescription trackDescription : this.mediaPlayer.audio().trackDescriptions()) {
            MenuItem item = initAudioMenuItem(mediaPlayer, trackDescription);
            audioMenu.getItems().add(item);
        }
        return audioMenu;
    }

    private MenuItem initAudioMenuItem(EmbeddedMediaPlayer mediaPlayer, TrackDescription trackDescription) {
        MenuItem item = new MenuItem(trackDescription.description());
        item.setId(trackDescription.id() + "");
        if (trackDescription.id() == mediaPlayer.audio().track()) {
            highlightMenuItem(item);
        }
        item.setOnAction(event -> setAudioTrack(mediaPlayer, trackDescription.id(), item));
        return item;
    }

    private void setAudioTrack(EmbeddedMediaPlayer mediaPlayer, int trackId, MenuItem item) {
        resetStyleOfCurrentAudioTrack(mediaPlayer);
        mediaPlayer.audio().setTrack(trackId);
        highlightMenuItem(item);
        this.mediaEntry.setAudioTrack(trackId + "");
    }

    private void highlightMenuItem(MenuItem item) {
        item.setStyle("-fx-text-fill: green;");
    }

    private void resetStyleOfCurrentAudioTrack(final EmbeddedMediaPlayer mediaPlayer) {
        Menu audioMenu = (Menu) this.contextMenu.getItems().get(0);
        audioMenu.getItems().filtered(item1 -> item1.getId().equals(
                mediaPlayer.audio().track() + "")).get(0).setStyle("-fx-text-fill: black;");
    }

    private void resetStyleOfCurrentSubtitleTrack(final EmbeddedMediaPlayer mediaPlayer) {
        Menu subtitleMenu = (Menu) this.contextMenu.getItems().get(1);
        subtitleMenu.getItems().filtered(item1 -> item1.getId().equals(
                mediaPlayer.subpictures().track() + "")).get(0).setStyle("-fx-text-fill: black;");
    }

    private class JavaFxVideoSurface extends CallbackVideoSurface {

        JavaFxVideoSurface() {
            super(new JavaFxBufferFormatCallback(), new JavaFxRenderCallback(), true, VideoSurfaceAdapters.getVideoSurfaceAdapter());
        }

    }

    private class JavaFxBufferFormatCallback extends BufferFormatCallbackAdapter {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            JavaFXDirectRenderingScene.this.img = new WritableImage(sourceWidth, sourceHeight);
            JavaFXDirectRenderingScene.this.pixelWriter = JavaFXDirectRenderingScene.this.img.getPixelWriter();

            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }
    }

    // Semaphore used to prevent the pixel writer from being updated in one thread while it is being rendered by a
    // different thread
    private final Semaphore semaphore = new Semaphore(1);

    // This is correct as far as it goes, but we need to use one of the timers to get smooth rendering (the timer is
    // handled by the demo sub-classes)
    private class JavaFxRenderCallback implements RenderCallback {
        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            try {
                JavaFXDirectRenderingScene.this.semaphore.acquire();
                JavaFXDirectRenderingScene.this.pixelWriter.setPixels(0, 0, bufferFormat.getWidth(), bufferFormat.getHeight(), JavaFXDirectRenderingScene.this.pixelFormat, nativeBuffers[0], bufferFormat.getPitches()[0]);
                JavaFXDirectRenderingScene.this.semaphore.release();
            } catch (InterruptedException e) {
                JavaFXDirectRenderingScene.LOGGER.error("JavaFxRenderCallback.display", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean startTimer() {
        this.nanoTimer.start();
        // wait for the media to load, because the api call to vlc is async
        while (this.mediaPlayer.media().info().duration() == -1 || this.mediaPlayer.audio().volume() == -1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                JavaFXDirectRenderingScene.LOGGER.error("nanoTime: sleep", e);
                Thread.currentThread().interrupt();
            }
        }
        return true;
    }

    private void stopTimer() {
        this.nanoTimer.cancel();
    }
}
