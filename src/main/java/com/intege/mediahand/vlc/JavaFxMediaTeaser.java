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

    private final ImageView thumbnailView;

    private final ImageView videoView;

    public JavaFxMediaTeaser() {
        super();

        this.mediaPlayerFactory = new MediaPlayerFactory();
        this.embeddedMediaPlayer = this.mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        this.thumbnailView = new ImageView();
        this.thumbnailView.setVisible(false);
        this.thumbnailView.setPreserveRatio(true);
        this.videoView = new ImageView();
        this.videoView.setPreserveRatio(true);
        this.embeddedMediaPlayer.videoSurface().set(ImageViewVideoSurfaceFactory.videoSurfaceForImageView(this.videoView));

        this.videoView.fitWidthProperty().bind(widthProperty());
        this.videoView.fitHeightProperty().bind(heightProperty());

        this.thumbnailView.fitWidthProperty().bind(widthProperty());
        this.thumbnailView.fitHeightProperty().bind(heightProperty());

        getChildren().add(this.videoView);
        getChildren().add(this.thumbnailView);
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

    public ImageView getThumbnailView() {
        return this.thumbnailView;
    }

    public void switchImageView(final boolean playTeaser) {
        if (playTeaser) {
            this.videoView.setVisible(true);
            this.thumbnailView.setVisible(false);
        } else {
            this.videoView.setVisible(false);
            this.thumbnailView.setVisible(true);
        }
    }

}
