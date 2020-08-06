package com.intege.mediahand.controller;

import org.springframework.stereotype.Component;

import com.intege.mediahand.core.JFxMediaHandApplication;
import com.intege.mediahand.domain.old.MediaEntry;
import com.intege.mediahand.repository.RepositoryFactory;

import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("rootLayout.fxml")
public class RootLayoutController {

    public void addDirectory() {
        if (JFxMediaHandApplication.addBasePath()) {
            JFxMediaHandApplication.getMediaHandAppController().fillTableView(RepositoryFactory.getMediaRepository().findAll());
        }
    }

    public void loadNewMediaEntries() {
        JFxMediaHandApplication.getMediaLoader().addAllMedia();
        JFxMediaHandApplication.getMediaHandAppController().fillTableView(RepositoryFactory.getMediaRepository().findAll());
    }

    public void onRemove() {
        MediaEntry selectedMediaEntry = JFxMediaHandApplication.getMediaHandAppController().getSelectedMediaEntry();
        if (selectedMediaEntry != null) {
            RepositoryFactory.getMediaRepository().remove(selectedMediaEntry);
        }
    }

    public void addMedia() {
        JFxMediaHandApplication.getMediaLoader().addSingleMedia();
        JFxMediaHandApplication.getMediaHandAppController().fillTableView(RepositoryFactory.getMediaRepository().findAll());
    }

}
