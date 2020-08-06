package com.intege.mediahand.domain;

import java.io.File;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.intege.mediahand.WatchState;
import com.intege.utils.Check;

@Entity
public class MediaEntry {

    /**
     * Id in the database.
     */
    @Id
    @GeneratedValue
    private int id;

    /**
     * Title of the media (most likely the name of the directory).
     */
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
     * True if the media is available and can be played (e.g. false, if an external drive is not connected).
     */
    private boolean available;

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

    public MediaEntry(String title) {
        this(0, title, 0, null, null, 0, null, 0, null, 0, null, 0, null, 0, null, null);
    }

    public MediaEntry(int id, String title, int episodeNumber, String mediaType, WatchState watchState, int rating, String path, int currentEpisode, LocalDate added, int episodeLength, LocalDate watchedDate, int watchedCount, DirectoryEntry basePath, int volume, String audioTrack, String subtitleTrack) {
        Check.notNullArgument(title, "title");

        this.id = id;
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

    public String getBasePathId() {
        if (this.basePath != null) {
            return this.basePath.getId() + "";
        } else {
            return null;
        }
    }

    public String getAbsolutePath() {
        if (this.basePath != null) {
            return this.getBasePath().getPath() + this.path;
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

    public int getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getEpisodeNumber() {
        return this.episodeNumber;
    }

    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public WatchState getWatchState() {
        return this.watchState;
    }

    public void setWatchState(WatchState watchState) {
        this.watchState = watchState;
    }

    public int getRating() {
        return this.rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCurrentEpisodeNumber() {
        return this.currentEpisode;
    }

    public void setCurrentEpisodeNumber(int currentEpisode) {
        this.currentEpisode = currentEpisode;
    }

    public String getCurrentEpisode() {
        return this.currentEpisode + "/" + this.episodeNumber;
    }

    public LocalDate getAdded() {
        return this.added;
    }

    public void setAdded(LocalDate added) {
        this.added = added;
    }

    public int getEpisodeLength() {
        return this.episodeLength;
    }

    public void setEpisodeLength(int episodeLength) {
        this.episodeLength = episodeLength;
    }

    public LocalDate getWatchedDate() {
        return this.watchedDate;
    }

    public void setWatchedDate(LocalDate watchedDate) {
        this.watchedDate = watchedDate;
    }

    public int getWatchedCount() {
        return this.watchedCount;
    }

    public void setWatchedCount(int watchedCount) {
        this.watchedCount = watchedCount;
    }

    public DirectoryEntry getBasePath() {
        return this.basePath;
    }

    public void setBasePath(DirectoryEntry basePath) {
        this.basePath = basePath;
    }

    public boolean isAvailable() {
        return this.available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getVolume() {
        return this.volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public String getAudioTrack() {
        return this.audioTrack;
    }

    public void setAudioTrack(final String audioTrack) {
        this.audioTrack = audioTrack;
    }

    public String getSubtitleTrack() {
        return this.subtitleTrack;
    }

    public void setSubtitleTrack(final String subtitleTrack) {
        this.subtitleTrack = subtitleTrack;
    }

    @Override
    public String toString() {
        return this.title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MediaEntry that = (MediaEntry) o;
        return this.id == that.id &&
                this.episodeNumber == that.episodeNumber &&
                this.rating == that.rating &&
                this.currentEpisode == that.currentEpisode &&
                this.episodeLength == that.episodeLength &&
                this.watchedCount == that.watchedCount &&
                this.available == that.available &&
                this.volume == that.volume &&
                this.title.equals(that.title) &&
                Objects.equals(this.audioTrack, that.audioTrack) &&
                Objects.equals(this.subtitleTrack, that.subtitleTrack) &&
                this.watchState == that.watchState &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.added, that.added) &&
                Objects.equals(this.watchedDate, that.watchedDate) &&
                Objects.equals(this.basePath, that.basePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.title, this.episodeNumber, this.mediaType, this.watchState, this.rating, this.path, this.currentEpisode, this.added, this.episodeLength, this.watchedDate, this.watchedCount, this.basePath, this.available, this.volume);
    }
}
