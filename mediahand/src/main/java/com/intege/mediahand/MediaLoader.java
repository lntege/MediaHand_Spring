package com.intege.mediahand;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intege.mediahand.controller.MediaHandAppController;
import com.intege.mediahand.core.JFxMediaHandApplication;
import com.intege.mediahand.domain.old.DirectoryEntry;
import com.intege.mediahand.domain.old.MediaEntry;
import com.intege.mediahand.repository.RepositoryFactory;
import com.intege.mediahand.repository.base.BaseRepository;
import com.intege.utils.Check;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * Loads media into the database.
 *
 * @author Lueko
 */
public class MediaLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaLoader.class);

    private DirectoryEntry basePath;

    /**
     * Adds all media of every directory path in dirTable into the mediaTable.
     */
    public void addAllMedia() {
        List<DirectoryEntry> basePaths = RepositoryFactory.getBasePathRepository().findAll();

        for (DirectoryEntry path : basePaths) {
            addMedia(path);
        }

    }

    public void addMedia(final DirectoryEntry basePath) {
        Check.notNullArgument(basePath, "basePath");

        this.basePath = basePath;
        addMedia(basePath.getPath());
    }

    /**
     * Adds all media in a directory into mediaTable.
     *
     * @param path Directory with media inside.
     */
    private void addMedia(final String path) {
        File f;
        Path p;

        f = new File(path);
        if (f.exists()) {
            p = f.toPath();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {
                for (Path dir : stream) {
                    if (dir.toFile().isDirectory()) {
                        // recursive execution
                        addMedia(dir.toString());
                    } else if (getMediaCount(dir.getParent().toFile()) > 0) {
                        MediaEntry newMediaEntry = createTempMediaEntry(dir.getParent(), this.basePath);
                        addSingleMedia(newMediaEntry);
                        break;
                    }
                }
            } catch (IOException e) {
                MediaLoader.LOGGER.warn("Could not add media", e);
            }
        }
    }

    public void addSingleMedia() {
        DirectoryEntry basePath = RepositoryFactory.getBasePathRepository().findAll().get(0);
        Optional<File> optionalDir = JFxMediaHandApplication.chooseMediaDirectory(Path.of(basePath.getPath()));
        if (optionalDir.isPresent()) {
            File dir = optionalDir.get();
            MediaEntry tempMediaEntry = createTempMediaEntry(dir.toPath(), null);
            addSingleMedia(tempMediaEntry);
        }
    }

    /**
     * Add a new {@link MediaEntry} to the database if a {@link MediaEntry} with the same name does not exist. Else update
     * the {@link MediaEntry}'s episode number. Update the base path if the media entry is currently not available
     * (because of a changed base path).
     *
     * @param newMediaEntry the {@link MediaEntry} to add
     */
    private void addSingleMedia(final MediaEntry newMediaEntry) {
        BaseRepository<MediaEntry> mediaRepository = RepositoryFactory.getMediaRepository();
        ObservableList<MediaEntry> mediaEntries = MediaHandAppController.getMediaEntries();
        FilteredList<MediaEntry> mediaEntryFilteredList = null;
        if (mediaEntries != null) {
            mediaEntryFilteredList = mediaEntries
                    .filtered(m -> m.getTitle().equals(newMediaEntry.getTitle()));
        }
        if (mediaEntryFilteredList == null || mediaEntryFilteredList.isEmpty()) {
            mediaRepository.create(newMediaEntry);
        } else {
            MediaEntry mediaEntry = mediaEntryFilteredList.get(0);
            if (!mediaEntry.isAvailable()) {
                mediaEntry.setBasePath(newMediaEntry.getBasePath());
                mediaEntry.setPath(newMediaEntry.getPath());
                mediaEntry.setMediaType(newMediaEntry.getMediaType());
                mediaEntry.setEpisodeNumber(newMediaEntry.getEpisodeNumber());
                mediaEntry.setAvailable(true);
                updateMediaEntry(mediaEntry, mediaRepository);
            } else {
                updateMediaEntryEpisodes(newMediaEntry, mediaRepository, mediaEntry);
            }
        }
    }

    public MediaEntry createTempMediaEntry(final Path mediaDirectory) {
        List<DirectoryEntry> allBasePaths = RepositoryFactory.getBasePathRepository().findAll();
        String basePath = mediaDirectory.getParent().getParent().toString();
        Optional<DirectoryEntry> optionalBasePath = allBasePaths.stream().filter(directoryEntry -> directoryEntry.getPath().equals(basePath)).findFirst();
        if (optionalBasePath.isEmpty()) {
            optionalBasePath = Optional.of(RepositoryFactory.getBasePathRepository().create(new DirectoryEntry(basePath)));
        }
        String mediaTitle = mediaDirectory.getFileName().toString();
        int episodeNumber = getMediaCount(mediaDirectory.toFile());
        String mediaType = mediaDirectory.getParent().getFileName().toString();
        String relativePath = mediaDirectory.toString().substring(optionalBasePath.get().getPath().length());
        return new MediaEntry(0, mediaTitle, episodeNumber, mediaType,
                WatchState.WANT_TO_WATCH, 0, relativePath, 0, null, 0, null, 0, optionalBasePath.get(), 50, null, null);
    }

    private MediaEntry createTempMediaEntry(final Path mediaDirectory, final DirectoryEntry basePath) {
        String mediaTitle = mediaDirectory.getFileName().toString();
        int episodeNumber = getMediaCount(mediaDirectory.toFile());
        String mediaType = mediaDirectory.getParent().getFileName().toString();
        String relativePath = mediaDirectory.toString();
        if (basePath != null) {
            relativePath = relativePath.substring(basePath.getPath().length());
        }
        return new MediaEntry(0, mediaTitle, episodeNumber, mediaType,
                WatchState.WANT_TO_WATCH, 0, relativePath, 0, null, 0, null, 0, basePath, 50, null, null);
    }

    private void updateMediaEntryEpisodes(final MediaEntry newMediaEntry, final BaseRepository<MediaEntry> mediaRepository, final MediaEntry mediaEntry) {
        mediaEntry.setEpisodeNumber(newMediaEntry.getEpisodeNumber());
        if (mediaEntry.getCurrentEpisodeNumber() > mediaEntry.getEpisodeNumber()) {
            mediaEntry.setCurrentEpisodeNumber(mediaEntry.getEpisodeNumber());
        }
        mediaRepository.update(mediaEntry);
    }

    private void updateMediaEntry(final MediaEntry mediaEntry, final BaseRepository<MediaEntry> mediaRepository) {
        mediaRepository.update(mediaEntry);
    }

    public void updateMediaEntry(final MediaEntry newMediaEntry, final BaseRepository<MediaEntry> mediaRepository, final MediaEntry mediaEntry) {
        mediaEntry.setTitle(newMediaEntry.getTitle());
        mediaEntry.setBasePath(newMediaEntry.getBasePath());
        mediaEntry.setPath(newMediaEntry.getPath());
        mediaEntry.setEpisodeNumber(newMediaEntry.getEpisodeNumber());
        mediaEntry.setMediaType(newMediaEntry.getMediaType());
        if (mediaEntry.getCurrentEpisodeNumber() > mediaEntry.getEpisodeNumber()) {
            mediaEntry.setCurrentEpisodeNumber(mediaEntry.getEpisodeNumber());
        }
        mediaRepository.update(mediaEntry);
    }

    public File getEpisode(final String absolutePath, final int episode) throws IOException {
        File dir = new File(absolutePath);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles((dir2, name) -> isMedia(name));
            if (files != null && files.length > episode - 1) {
                return files[episode - 1];
            } else {
                throw new IOException("Episode " + episode + " does not exist in \"" + absolutePath + "\".");
            }
        } else {
            throw new IOException("\"" + absolutePath + "\" does not exist or is not a directory.");
        }
    }

    private int getMediaCount(final File dir) {
        return Objects.requireNonNull(dir.listFiles((dir1, name) -> isMedia(name))).length;
    }

    private boolean isMedia(final String name) {
        return name.contains(".mkv") || name.contains(".mp4") || name.contains(".flv");
    }

}
