package com.intege.mediahand.controller;

import org.springframework.stereotype.Component;

import com.intege.mediahand.core.JfxMediaHandApplication;
import com.intege.mediahand.domain.old.MediaEntry;
import com.intege.mediahand.repository.RepositoryFactory;

import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("rootLayout.fxml")
public class RootLayoutController {

    public void addDirectory() {
        if (JfxMediaHandApplication.addBasePath()) {
            JfxMediaHandApplication.getMediaHandAppController().fillTableView(RepositoryFactory.getMediaRepository().findAll());
        }
    }

    public void loadNewMediaEntries() {
        JfxMediaHandApplication.getMediaLoader().addAllMedia();
        JfxMediaHandApplication.getMediaHandAppController().fillTableView(RepositoryFactory.getMediaRepository().findAll());
    }

    public void onRemove() {
        MediaEntry selectedMediaEntry = JfxMediaHandApplication.getMediaHandAppController().getSelectedMediaEntry();
        if (selectedMediaEntry != null) {
            RepositoryFactory.getMediaRepository().remove(selectedMediaEntry);
        }
    }

    public void addMedia() {
        JfxMediaHandApplication.getMediaLoader().addSingleMedia();
        JfxMediaHandApplication.getMediaHandAppController().fillTableView(RepositoryFactory.getMediaRepository().findAll());
    }

}
