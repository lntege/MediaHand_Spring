package com.intege.mediahand.repository;

public class RepositoryFactory {

    // replace implementation with another one if necessary
    private static final MediaRepository MEDIA_REPOSITORY = new MediaRepository();
    private static final SettingsRepository SETTINGS_REPOSITORY = new SettingsRepository();
    private static final BasePathRepository BASE_PATH_REPOSITORY = new BasePathRepository();

    public static MediaRepository getMediaRepository() {
        return RepositoryFactory.MEDIA_REPOSITORY;
    }

    public static SettingsRepository getSettingsRepository() {
        return RepositoryFactory.SETTINGS_REPOSITORY;
    }

    public static BasePathRepository getBasePathRepository() {
        return RepositoryFactory.BASE_PATH_REPOSITORY;
    }

}
