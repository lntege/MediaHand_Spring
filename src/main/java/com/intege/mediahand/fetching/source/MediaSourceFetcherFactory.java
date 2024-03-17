package com.intege.mediahand.fetching.source;

public class MediaSourceFetcherFactory {

    private static VoeFetcher voeFetcherInstance;

    public static MediaSourceFetcher getMediaSourceFetcher(final SourceType mediaType) {
        switch (mediaType) {
            case VOE:
                return getVoeFetcherInstance();
            default:
                return getVoeFetcherInstance();

        }
    }

    private static VoeFetcher getVoeFetcherInstance() {
        if (voeFetcherInstance == null) {
            voeFetcherInstance = new VoeFetcher();
        }
        return voeFetcherInstance;
    }

}
