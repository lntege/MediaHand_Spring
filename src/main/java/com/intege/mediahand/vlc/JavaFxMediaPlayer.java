package com.intege.mediahand.vlc;

import java.io.File;

import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.Getter;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurfaceFactory;
import uk.co.caprica.vlcj.player.base.EventApi;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class JavaFxMediaPlayer implements MediaPlayerComponent {

    private final MediaPlayerFactory mediaPlayerFactory;

    private final ImageView imageView;

    @Getter
    private final EmbeddedMediaPlayer embeddedMediaPlayer;

    @Getter
    private final Scene scene;

    @Getter
    private final StackPane stackPane;

    public JavaFxMediaPlayer() {
        this.mediaPlayerFactory = new MediaPlayerFactory();
        this.embeddedMediaPlayer = this.mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        this.imageView = new ImageView();
        this.imageView.setPreserveRatio(true);
        this.embeddedMediaPlayer.videoSurface().set(ImageViewVideoSurfaceFactory.videoSurfaceForImageView(this.imageView));

        this.stackPane = buildStackPane();
        this.scene = new Scene(this.stackPane, Color.BLACK);
    }

    public EventApi events() {
        return this.embeddedMediaPlayer.events();
    }

    public boolean start(final File media) {
        return this.embeddedMediaPlayer.media().start(media.getAbsolutePath());
    }

    public boolean start(final String hslStream) {
        return this.embeddedMediaPlayer.media().start(hslStream);
    }

    @Override
    public void stop() {
        this.embeddedMediaPlayer.release();
        this.mediaPlayerFactory.release();
    }

    private StackPane buildStackPane() {
        StackPane stackPane = new StackPane();

        this.imageView.fitWidthProperty().bind(stackPane.widthProperty());
        this.imageView.fitHeightProperty().bind(stackPane.heightProperty());

        stackPane.getChildren().add(this.imageView);
        stackPane.setStyle("-fx-background-color: rgb(0, 0, 0);");

        return stackPane;
    }

}
