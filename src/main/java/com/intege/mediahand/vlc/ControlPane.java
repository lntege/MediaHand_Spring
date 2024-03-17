package com.intege.mediahand.vlc;

import java.util.Timer;
import java.util.TimerTask;

import com.intege.mediahand.core.JfxMediaHandApplication;
import com.intege.mediahand.domain.MediaEntry;
import com.intege.mediahand.domain.repository.MediaEntryRepository;
import com.intege.mediahand.utils.MessageUtil;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerUnpluggedException;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

@Slf4j
public class ControlPane implements MediaPlayerComponent {

    private static final long TIME_SLIDER_DELAY = 1000;

    @Getter
    private final BorderPane borderPane;

    private final EmbeddedMediaPlayer embeddedMediaPlayer;

    private final MediaEntryRepository mediaEntryRepository;

    private final JfxMediaHandApplication jfxMediaHandApplication;

    private Slider mediaTimeSlider;

    private MediaEntry mediaEntry;

    private Timer timer = new Timer();

    private ControllerIndex currentController;

    private boolean isRunning;

    private Slider volumeSlider;

    public ControlPane(final EmbeddedMediaPlayer embeddedMediaPlayer, final JfxMediaHandApplication jfxMediaHandApplication, final Scene scene,
                       final MediaEntryRepository mediaEntryRepository) {
        this.borderPane = new BorderPane();
        this.embeddedMediaPlayer = embeddedMediaPlayer;
        this.jfxMediaHandApplication = jfxMediaHandApplication;
        this.mediaEntryRepository = mediaEntryRepository;

        initControllerControl();
        addTimeListener();
        addKeyControlListeners(scene);
    }

    public void update(final MediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
        long duration = this.embeddedMediaPlayer.media().info().duration();
        if (duration == 0) {
            duration = 60000 * 25;
        }
        double mediaDuration = duration / 60000.0;
        BorderPane sliderPane = initSliderPane(mediaDuration);
        showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY * 3);

        this.borderPane.setBottom(sliderPane);
    }

    private void initControllerControl() {
        ControllerManager controllerManager = new ControllerManager();
        controllerManager.initSDLGamepad();
        this.currentController = controllerManager.getControllerIndex(0);
        Thread thread = new Thread(() -> {
            this.isRunning = true;
            while (this.isRunning) {
                controllerManager.update();
                try {
                    if (this.currentController.isButtonJustPressed(ControllerButton.B)) {
                        this.embeddedMediaPlayer.controls().pause();
                        showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY * 3);
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.RIGHTBUMPER)) {
                        Platform.runLater(this::playNextEpisode);
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.LEFTBUMPER)) {
                        Platform.runLater(this::playPreviousEpisode);
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.X)) {
                        Platform.runLater(() -> this.embeddedMediaPlayer.controls().skipTime(80000));
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.BACK)) {
                        Platform.runLater(() -> {
                            stop();
                            this.jfxMediaHandApplication.setDefaultScene();
                        });
                    }
                    if (this.currentController.isButtonPressed(ControllerButton.DPAD_LEFT)) {
                        showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY);
                        if (this.currentController.isButtonPressed(ControllerButton.A)) {
                            Platform.runLater(() -> this.embeddedMediaPlayer.controls().skipTime(-3000));
                        } else {
                            Platform.runLater(() -> this.embeddedMediaPlayer.controls().skipTime(-1000));
                        }
                    }
                    if (this.currentController.isButtonPressed(ControllerButton.DPAD_RIGHT)) {
                        showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY);
                        if (this.currentController.isButtonPressed(ControllerButton.A)) {
                            Platform.runLater(() -> this.embeddedMediaPlayer.controls().skipTime(3000));
                        } else {
                            Platform.runLater(() -> this.embeddedMediaPlayer.controls().skipTime(1000));
                        }
                    }
                    if (this.currentController.isButtonJustPressed(ControllerButton.Y)) {
                        Platform.runLater(() -> this.jfxMediaHandApplication.getStage().setFullScreen(!this.jfxMediaHandApplication.getStage().isFullScreen()));
                    }
                } catch (ControllerUnpluggedException e) {
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    ControlPane.log.error("Controller thread: sleep", e);
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
    }

    private void addKeyControlListeners(final Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stop();
                this.jfxMediaHandApplication.setDefaultScene();
            } else if (event.getCode() == KeyCode.SPACE) {
                this.embeddedMediaPlayer.controls().pause();
                showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY * 3);
            } else if (event.getCode() == KeyCode.ENTER) {
                this.embeddedMediaPlayer.controls().skipTime(80000);
                updateMediaTimeSlider(this.embeddedMediaPlayer.status().time());
                showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY);
            } else if (!event.isControlDown() && event.getCode() == KeyCode.F) {
                this.jfxMediaHandApplication.getStage().setFullScreen(true);
            } else if (event.getCode() == KeyCode.UP) {
                playNextEpisode();
            } else if (event.getCode() == KeyCode.DOWN) {
                playPreviousEpisode();
            } else if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.ADD) {
                this.volumeSlider.setValue(this.volumeSlider.getValue() + 5);
                showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY);
            } else if (event.getCode() == KeyCode.MINUS || event.getCode() == KeyCode.SUBTRACT) {
                this.volumeSlider.setValue(this.volumeSlider.getValue() - 5);
                showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY);
            } else if (event.getCode() == KeyCode.NUMPAD6 || event.getCode() == KeyCode.D) {
                this.embeddedMediaPlayer.controls().skipTime(2000);
                updateMediaTimeSlider(this.embeddedMediaPlayer.status().time());
                showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY);
            } else if (event.getCode() == KeyCode.NUMPAD4 || event.getCode() == KeyCode.A) {
                this.embeddedMediaPlayer.controls().skipTime(-2000);
                updateMediaTimeSlider(this.embeddedMediaPlayer.status().time());
                showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY);
            }
        });
    }

    @Override
    public void stop() {
        this.isRunning = false;
        if (this.embeddedMediaPlayer.status().isPlaying()) {
            this.embeddedMediaPlayer.controls().stop();
        }
        this.timer.cancel();
    }

    private void playSelectedMedia(boolean fullScreen) {
        this.jfxMediaHandApplication.getMediaHandAppController().playEmbeddedMedia();
        this.jfxMediaHandApplication.getStage().setFullScreen(fullScreen);
    }

    private void playNextEpisode() {
        stop();
        boolean fullScreen = this.jfxMediaHandApplication.getStage().isFullScreen();
        this.jfxMediaHandApplication.getMediaHandAppController().increaseCurrentEpisode();
        playSelectedMedia(fullScreen);
    }

    private void playPreviousEpisode() {
        stop();
        boolean fullScreen = this.jfxMediaHandApplication.getStage().isFullScreen();
        this.jfxMediaHandApplication.getMediaHandAppController().decreaseCurrentEpisode();
        playSelectedMedia(fullScreen);
    }

    private void addTimeListener() {
        this.embeddedMediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, long newTime) {
                Platform.runLater(() -> updateMediaTimeSlider(newTime));
            }
        });
    }

    private void updateMediaTimeSlider(long newTime) {
        if (this.mediaTimeSlider != null && !this.mediaTimeSlider.isPressed()) {
            this.mediaTimeSlider.setValue(newTime / 60000.0);
        }
    }

    private BorderPane initSliderPane(final double mediaDuration) {
        this.mediaTimeSlider = initMediaTimeSlider(mediaDuration);
        this.volumeSlider = initVolumeSlider(this.mediaEntry.getVolume());

        String length = ((int) (mediaDuration) + ":" + (int) ((mediaDuration % 1) * 60));
        Label currentTimeLabel = new Label(length);
        currentTimeLabel.setTextFill(Color.YELLOW);
        this.mediaTimeSlider.valueProperty().addListener((observable, oldValue, newValue) -> currentTimeLabel.setText(
                (int) (newValue.doubleValue()) + ":" + (int) ((newValue.doubleValue() % 1) * 60) + " - " + length));
        VBox vBox = new VBox(this.mediaTimeSlider, currentTimeLabel);
        vBox.setPadding(new Insets(3, 0, 5, 5));

        BorderPane sliderPane = new BorderPane(vBox);
        sliderPane.setRight(this.volumeSlider);

        registerControlPaneMouseListener(sliderPane);
        return sliderPane;
    }

    private void registerControlPaneMouseListener(final BorderPane sliderPane) {
        Parent parent = this.borderPane.getParent();
        if (parent != null) {
            parent.setOnMouseMoved(event -> {
                showTimedTimeSlider(ControlPane.TIME_SLIDER_DELAY);
                TimerTask timerTask = showTimeSlider();
                if (sliderPane.sceneToLocal(event.getSceneX(), event.getSceneY()).getY() < -10) {
                    this.timer = new Timer();
                    this.timer.schedule(timerTask, ControlPane.TIME_SLIDER_DELAY);
                }
            });
        } else {
            MessageUtil.warningAlert("Control pane", "Could not add control pane, because it has not been added to a scene graph.");
        }
    }

    private Slider initMediaTimeSlider(final double mediaDuration) {
        Slider slider = new Slider(0, mediaDuration, 0);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setOnMouseClicked(event -> this.embeddedMediaPlayer.controls().setTime((long) (slider.getValue() * 60000)));
        return slider;
    }

    private Slider initVolumeSlider(final int volume) {
        Slider volumeSlider = new Slider(0, 100, volume);
        this.embeddedMediaPlayer.audio().setVolume((int) volumeSlider.getValue());
        volumeSlider.setOnMouseClicked(event -> {
            int newVolume = (int) volumeSlider.getValue();
            this.embeddedMediaPlayer.audio().setVolume(newVolume);
            this.mediaEntry.setVolume(newVolume);
            this.mediaEntryRepository.save(this.mediaEntry);
        });
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.embeddedMediaPlayer.audio().setVolume(newValue.intValue());
            this.mediaEntry.setVolume(newValue.intValue());
            this.mediaEntryRepository.save(this.mediaEntry);
        });
        return volumeSlider;
    }

    private void showTimedTimeSlider(final long delay) {
        TimerTask timerTask = showTimeSlider();
        this.timer = new Timer();
        this.timer.schedule(timerTask, delay);
    }

    private TimerTask showTimeSlider() {
        this.borderPane.setVisible(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ControlPane.this.borderPane.setVisible(false);
            }
        };
        this.timer.cancel();
        return task;
    }

}
