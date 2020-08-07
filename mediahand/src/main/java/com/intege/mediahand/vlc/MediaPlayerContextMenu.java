package com.intege.mediahand.vlc;

import java.util.Optional;

import com.intege.mediahand.domain.MediaEntry;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import uk.co.caprica.vlcj.player.base.TrackDescription;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class MediaPlayerContextMenu {

    private final ContextMenu contextMenu;

    private final EmbeddedMediaPlayer embeddedMediaPlayer;

    private MediaEntry mediaEntry;

    private final Pane parent;

    public MediaPlayerContextMenu(final EmbeddedMediaPlayer embeddedMediaPlayer, final Pane parent) {
        this.contextMenu = new ContextMenu();
        this.embeddedMediaPlayer = embeddedMediaPlayer;
        this.parent = parent;
    }

    public ContextMenu getContextMenu() {
        return this.contextMenu;
    }

    public void update(final MediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
        buildContextMenu();
        registerContextMenuListeners();
    }

    private void registerContextMenuListeners() {
        registerShowContextMenuListener();
        registerHideContextMenuListener();
    }

    private void registerShowContextMenuListener() {
        this.parent.setOnContextMenuRequested(event -> this.contextMenu.show(this.parent, event.getScreenX(), event.getScreenY()));
    }

    private void registerHideContextMenuListener() {
        this.parent.setOnMouseClicked(event -> this.contextMenu.hide());
    }

    private void buildContextMenu() {
        Menu audioMenu = buildAudioContextMenu(this.embeddedMediaPlayer);

        Menu subtitleMenu = buildSubtitleContextMenu(this.embeddedMediaPlayer);

        this.contextMenu.getItems().retainAll(audioMenu, subtitleMenu);
        this.contextMenu.getItems().addAll(audioMenu, subtitleMenu);

        if (this.mediaEntry.getSubtitleTrack() != null) {
            Optional<MenuItem> menuItem = subtitleMenu.getItems().stream().filter(item -> item.getId().equals(this.mediaEntry.getSubtitleTrack())).findFirst();
            menuItem.ifPresent(item -> setSubtitleTrack(this.embeddedMediaPlayer, Integer.parseInt(this.mediaEntry.getSubtitleTrack()), item));
        }
        if (this.mediaEntry.getAudioTrack() != null) {
            Optional<MenuItem> menuItem = audioMenu.getItems().stream().filter(item -> item.getId().equals(this.mediaEntry.getAudioTrack())).findFirst();
            menuItem.ifPresent(item -> setAudioTrack(this.embeddedMediaPlayer, Integer.parseInt(this.mediaEntry.getAudioTrack()), item));
        }
    }

    private Menu buildSubtitleContextMenu(EmbeddedMediaPlayer mediaPlayer) {
        Menu subtitleMenu = new Menu("Subtitle");
        for (TrackDescription trackDescription : this.embeddedMediaPlayer.subpictures().trackDescriptions()) {
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
        for (TrackDescription trackDescription : this.embeddedMediaPlayer.audio().trackDescriptions()) {
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

}
