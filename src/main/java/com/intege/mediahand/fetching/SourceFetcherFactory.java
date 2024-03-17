package com.intege.mediahand.fetching;

public class SourceFetcherFactory {

    private static AniworldFetcher aniworldFetcherInstance;

    public static SourceFetcher getAniworldFetcherInstance() {
        if (aniworldFetcherInstance == null) {
            aniworldFetcherInstance = new AniworldFetcher();
        }
        return aniworldFetcherInstance;
    }

}
