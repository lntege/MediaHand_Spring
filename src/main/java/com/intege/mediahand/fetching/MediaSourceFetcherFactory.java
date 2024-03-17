package com.intege.mediahand.fetching;

public class MediaSourceFetcherFactory {

    private static VoeFetcher voeFetcherInstance;

    public static MediaSourceFetcher getMediaSourceFetcher(final SourceType mediaType) {
        if (mediaType == SourceType.VOE) {
            return getVoeFetcherInstance();
        }
        return null;
    }

    private static VoeFetcher getVoeFetcherInstance() {
        if (voeFetcherInstance == null) {
            voeFetcherInstance = new VoeFetcher();
        }
        return voeFetcherInstance;
    }

}
