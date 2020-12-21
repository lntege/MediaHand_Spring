package com.intege.mediahand;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.intege.mediahand.controller.MediaHandAppController;
import com.intege.mediahand.core.JfxMediaHandApplication;
import com.intege.mediahand.domain.DirectoryEntry;
import com.intege.mediahand.domain.MediaEntry;
import com.intege.mediahand.domain.repository.DirectoryEntryRepository;
import com.intege.mediahand.domain.repository.MediaEntryRepository;
import com.intege.utils.Check;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads media into the database.
 *
 * @author Lueko
 */
@Slf4j
@Service
public class MediaLoader {

    private DirectoryEntry basePath;

    @Autowired
    private DirectoryEntryRepository directoryEntryRepository;

    @Autowired
    private MediaEntryRepository mediaEntryRepository;

    @Lazy
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private JfxMediaHandApplication jfxMediaHandApplication;

    /**
     * Adds all media of every directory path in dirTable into the mediaTable.
     */
    public void addAllMedia() {
        List<DirectoryEntry> basePaths = this.directoryEntryRepository.findAll();

        for (DirectoryEntry path : basePaths) {
            addMedia(path);
        }

    }

    @Transactional
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
                MediaLoader.log.warn("Could not add media", e);
            }
        }
    }

    @Transactional
    public void addSingleMedia() {
        DirectoryEntry basePath = this.directoryEntryRepository.findAll().get(0);
        Optional<File> optionalDir = this.jfxMediaHandApplication.chooseMediaDirectory(Path.of(basePath.getPath()));
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
        ObservableList<MediaEntry> mediaEntries = MediaHandAppController.getMediaEntries();
        FilteredList<MediaEntry> mediaEntryFilteredList = null;
        if (mediaEntries != null) {
            mediaEntryFilteredList = mediaEntries
                    .filtered(m -> m.getTitle().equals(newMediaEntry.getTitle()));
        }
        if (mediaEntryFilteredList == null || mediaEntryFilteredList.isEmpty()) {
            this.mediaEntryRepository.save(newMediaEntry);
        } else {
            MediaEntry mediaEntry = mediaEntryFilteredList.get(0);
            if (!mediaEntry.isAvailable()) {
                mediaEntry.setBasePath(newMediaEntry.getBasePath());
                mediaEntry.setPath(newMediaEntry.getPath());
                mediaEntry.setMediaType(newMediaEntry.getMediaType());
                mediaEntry.setEpisodeNumber(newMediaEntry.getEpisodeNumber());
                mediaEntry.setAvailable(true);
            } else {
                updateMediaEntryEpisodes(newMediaEntry, mediaEntry);
            }
            MediaHandAppController.triggerMediaEntryUpdate(mediaEntry);
        }
    }

    public MediaEntry createTempMediaEntry(final Path mediaDirectory) {
        List<DirectoryEntry> allBasePaths = this.directoryEntryRepository.findAll();
        String basePath = mediaDirectory.getParent().getParent().toString();
        Optional<DirectoryEntry> optionalBasePath = allBasePaths.stream().filter(directoryEntry -> directoryEntry.getPath().equals(basePath)).findFirst();
        if (optionalBasePath.isEmpty()) {
            optionalBasePath = Optional.of(this.directoryEntryRepository.save(new DirectoryEntry(basePath)));
        }
        String mediaTitle = mediaDirectory.getFileName().toString();
        int episodeNumber = getMediaCount(mediaDirectory.toFile());
        String mediaType = mediaDirectory.getParent().getFileName().toString();
        String relativePath = mediaDirectory.toString().substring(optionalBasePath.get().getPath().length());
        return new MediaEntry(mediaTitle, episodeNumber, mediaType, WatchState.WANT_TO_WATCH, 0, relativePath, 0, null, 0, null, 0, optionalBasePath.get(), 50, null, null);
    }

    private MediaEntry createTempMediaEntry(final Path mediaDirectory, final DirectoryEntry basePath) {
        String mediaTitle = mediaDirectory.getFileName().toString();
        int episodeNumber = getMediaCount(mediaDirectory.toFile());
        String mediaType = mediaDirectory.getParent().getFileName().toString();
        String relativePath = mediaDirectory.toString();
        if (basePath != null) {
            relativePath = relativePath.substring(basePath.getPath().length());
        }
        return new MediaEntry(mediaTitle, episodeNumber, mediaType, WatchState.WANT_TO_WATCH, 0, relativePath, 1, LocalDate.now(), 0, null, 0, basePath, 50, null, null);
    }

    private void updateMediaEntryEpisodes(final MediaEntry newMediaEntry, final MediaEntry mediaEntry) {
        mediaEntry.setEpisodeNumber(newMediaEntry.getEpisodeNumber());
        if (mediaEntry.getCurrentEpisode() > mediaEntry.getEpisodeNumber()) {
            mediaEntry.setCurrentEpisode(mediaEntry.getEpisodeNumber());
        }
    }

    @Transactional
    public void updateMediaEntry(final MediaEntry newMediaEntry, final MediaEntry mediaEntry) {
        mediaEntry.setTitle(newMediaEntry.getTitle());
        mediaEntry.setBasePath(newMediaEntry.getBasePath());
        mediaEntry.setPath(newMediaEntry.getPath());
        mediaEntry.setEpisodeNumber(newMediaEntry.getEpisodeNumber());
        mediaEntry.setMediaType(newMediaEntry.getMediaType());
        if (mediaEntry.getCurrentEpisode() > mediaEntry.getEpisodeNumber()) {
            mediaEntry.setCurrentEpisode(mediaEntry.getEpisodeNumber());
        }
        MediaHandAppController.triggerMediaEntryUpdate(mediaEntry);
    }

    public static File getEpisode(final String absolutePath, final int episode) throws IOException {
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

    private static boolean isMedia(final String name) {
        return name.contains(".mkv") || name.contains(".mp4") || name.contains(".flv");
    }

}
