package com.intege.mediahand.vlc.event;

import java.util.List;

import com.intege.mediahand.vlc.MediaPlayerComponent;

import javafx.event.EventHandler;
import javafx.stage.WindowEvent;

public class StopRenderingSceneHandler implements EventHandler<WindowEvent> {

    private final List<MediaPlayerComponent> mediaPlayerComponents;

    public StopRenderingSceneHandler(final List<MediaPlayerComponent> mediaPlayerComponents) {
        this.mediaPlayerComponents = mediaPlayerComponents;
    }

    @Override
    public void handle(WindowEvent event) {
        for (MediaPlayerComponent mediaPlayerComponent : this.mediaPlayerComponents) {
            mediaPlayerComponent.stop();
        }
    }

}
