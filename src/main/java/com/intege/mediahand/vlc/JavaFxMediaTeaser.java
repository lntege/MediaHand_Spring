package com.intege.mediahand.vlc;

import java.io.File;

import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurfaceFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class JavaFxMediaTeaser extends StackPane implements MediaPlayerComponent {

    private final MediaPlayerFactory mediaPlayerFactory;

    private final EmbeddedMediaPlayer embeddedMediaPlayer;

    public JavaFxMediaTeaser() {
        super();

        this.mediaPlayerFactory = new MediaPlayerFactory();
        this.embeddedMediaPlayer = this.mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        this.embeddedMediaPlayer.videoSurface().set(ImageViewVideoSurfaceFactory.videoSurfaceForImageView(imageView));

        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());

        getChildren().add(imageView);
    }

    public boolean start(final File media) {
        return this.embeddedMediaPlayer.media().start(media.getAbsolutePath());
    }

    public void pause() {
        if (this.embeddedMediaPlayer.status().isPlaying()) {
            this.embeddedMediaPlayer.controls().stop();
        }
    }

    @Override
    public void stop() {
        this.embeddedMediaPlayer.release();
        this.mediaPlayerFactory.release();
    }

    public EmbeddedMediaPlayer getEmbeddedMediaPlayer() {
        return this.embeddedMediaPlayer;
    }
}
