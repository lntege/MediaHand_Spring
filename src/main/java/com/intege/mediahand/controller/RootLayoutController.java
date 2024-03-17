package com.intege.mediahand.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.intege.mediahand.MediaLoader;
import com.intege.mediahand.core.JfxMediaHandApplication;
import com.intege.mediahand.domain.repository.MediaEntryRepository;
import com.intege.mediahand.utils.MessageUtil;

import javafx.event.ActionEvent;
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

    public void showHelp(final ActionEvent actionEvent) {
        MessageUtil.infoAlert("", "Table:\n    Enter - Play selected media using vlc player\n\nInline player:\n    Space - play/pause\n    F - fullscreen"
                + "\n    Ctrl+F - exit fullscreen\n    Enter - skip opening (1min 20s)\n    Up - next episode\n    Down - previous episode\n    Plus - volume up\n    Minus - volume down"
                + "\n    A / D - rewind/skip 2 seconds\n    Esc - exit inline player");
    }
}
