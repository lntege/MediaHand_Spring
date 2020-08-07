package com.intege.mediahand.domain;

import java.io.File;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

import com.intege.mediahand.WatchState;
import com.intege.utils.Check;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Entity
public @Data
class MediaEntry {

    /**
     * Id in the database.
     */
    @Id
    @GeneratedValue
    @Setter(AccessLevel.NONE)
    private int id;

    /**
     * Title of the media (most likely the name of the directory).
     */
    @Column(unique = true)
    private String title;

    /**
     * Number of episodes of the media.
     */
    private int episodeNumber;

    /**
     * Type of the media (OVA, Series, ...).
     */
    private String mediaType;

    /**
     * State of the media (Watching, Watched, ...).
     */
    @Enumerated(EnumType.STRING)
    private WatchState watchState;

    /**
     * Rating of the media based on the users opinion.
     */
    private int rating;

    /**
     * The path to the media directory relative to the base path.
     */
    private String path;

    /**
     * Current episode, the user wants to watch.
     */
    private int currentEpisode;

    /**
     * Date when the media was added to the database.
     */
    private LocalDate added;

    /**
     * Average length of the media per episode in minutes.
     */
    private int episodeLength;

    /**
     * Date when the user first watched the media.
     */
    private LocalDate watchedDate;

    /**
     * Count how many times the user watched the media.
     */
    private int watchedCount;

    /**
     * The base path of the media, where most likely are more media to find.
     */
    @ManyToOne
    private DirectoryEntry basePath;

    /**
     * Volume of the media player to use for this media.
     */
    private int volume;

    /**
     * The lastly selected audio track.
     */
    private String audioTrack;

    /**
     * The lastly selected subtitle track.
     */
    private String subtitleTrack;

    /**
     * True if the media is available and can be played (e.g. false, if an external drive is not connected).
     */
    @Transient
    private boolean available;

    MediaEntry() {
    }

    @PostLoad
    public void init() {
        this.available = new File(getAbsolutePath()).exists();
    }

    public MediaEntry(String title, int episodeNumber, String mediaType, WatchState watchState, int rating, String path, int currentEpisode, LocalDate added, int episodeLength,
                      LocalDate watchedDate, int watchedCount, DirectoryEntry basePath, int volume, String audioTrack, String subtitleTrack) {
        Check.notNullArgument(title, "title");

        this.title = title;
        this.episodeNumber = episodeNumber;
        this.mediaType = mediaType;
        this.watchState = watchState;
        this.rating = rating;
        this.path = path;
        this.currentEpisode = currentEpisode;
        this.added = added;
        this.episodeLength = episodeLength;
        this.watchedDate = watchedDate;
        this.watchedCount = watchedCount;
        this.basePath = basePath;
        this.volume = volume;
        this.audioTrack = audioTrack;
        this.subtitleTrack = subtitleTrack;

        this.available = new File(getAbsolutePath()).exists();
    }

    public String getAbsolutePath() {
        if (this.basePath != null) {
            return this.basePath.getPath() + this.path;
        } else {
            return Objects.requireNonNullElse(this.path, "");
        }
    }

    public boolean filterByWatchState(final String watchState) {
        return watchState.equals("ALL") || watchState.equals(this.watchState.toString());
    }

    public boolean filterByMediaType(final String mediaType) {
        return mediaType.equals("All") || mediaType.equals(this.mediaType);
    }

}
