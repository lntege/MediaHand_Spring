package com.intege.mediahand.controller;

import static com.intege.mediahand.domain.MediaEntry.MEDIATYPE_EXTERNAL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.intege.mediahand.MediaLoader;
import com.intege.mediahand.WatchState;
import com.intege.mediahand.core.JfxMediaHandApplication;
import com.intege.mediahand.domain.MediaEntry;
import com.intege.mediahand.domain.repository.MediaEntryRepository;
import com.intege.mediahand.fetching.SourceFetcherFactory;
import com.intege.mediahand.utils.MessageUtil;

import javafx.event.ActionEvent;
import javafx.scene.control.TextInputDialog;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("rootLayout.fxml")
public class RootLayoutController {

    @Lazy
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private JfxMediaHandApplication jfxMediaHandApplication;

    @Autowired
    private MediaLoader mediaLoader;

    @Autowired
    private MediaEntryRepository mediaEntryRepository;

    @Autowired
    private MediaHandAppController mediaHandAppController;

    public void addDirectory() {
        if (this.jfxMediaHandApplication.addBasePath()) {
            this.mediaHandAppController.fillTableView(this.mediaEntryRepository.findAll());
        }
    }

    public void loadNewMediaEntries() {
        this.mediaLoader.addAllMedia();
        this.mediaHandAppController.fillTableView(this.mediaEntryRepository.findAll());
    }

    public void addMedia() {
        this.mediaLoader.addSingleMedia();
        this.mediaHandAppController.fillTableView(this.mediaEntryRepository.findAll());
    }

    public void addExternalMedia() throws IOException {
        TextInputDialog td = new TextInputDialog("https://aniworld.to/anime/stream/shangri-la-frontier/staffel-1");
        td.setHeaderText("Enter URL to external media");
        td.showAndWait();
        String url = td.getEditor().getText();
        URL seasonUrl;
        try {
            seasonUrl = new URL(url);
        } catch (MalformedURLException e) {
            MessageUtil.infoAlert("Add external media", "Invalid URL. " + e.getMessage());
            return;
        }
        td = new TextInputDialog();
        td.setHeaderText("Enter a title");
        td.showAndWait();
        String title = td.getEditor().getText();
        if (title.isEmpty()) {
            MessageUtil.infoAlert("Add external media", "Title can not be empty");
            return;
        }
        List<URL> urls = SourceFetcherFactory.getAniworldFetcherInstance().extractEpisodes(seasonUrl);
        this.mediaLoader.addSingleMedia(new MediaEntry(title, urls.size(), MEDIATYPE_EXTERNAL, WatchState.WATCHING, 0, url, 1, LocalDate.now(), 0, null, 0, null, 50, null, null));
        this.mediaHandAppController.fillTableView(this.mediaEntryRepository.findAll());
    }

    public void showHelp(final ActionEvent actionEvent) {
        MessageUtil.infoAlert("",
                "Table:\n Enter - Play selected media using embedded player\n    Ctrl + Enter - Play selected media using vlc player\n\nInline player:\n    Space - play/pause\n    F - fullscreen"
                + "\n    Ctrl+F - exit fullscreen\n    Enter - skip opening (1min 20s)\n    Up - next episode\n    Down - previous episode\n    Plus - volume up\n    Minus - volume down"
                + "\n    A / D - rewind/skip 2 seconds\n    Esc - exit inline player");
    }
}
