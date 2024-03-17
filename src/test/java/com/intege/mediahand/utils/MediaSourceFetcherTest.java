package com.intege.mediahand.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.intege.mediahand.fetching.source.MediaSourceFetcher;
import com.intege.mediahand.fetching.source.VoeFetcher;

class MediaSourceFetcherTest {

    @Test
    void testExtractHlsUrl() throws IOException {
        // given
        MediaSourceFetcher mediaSourceFetcher = new VoeFetcher();
        String url = "https://vincentincludesuccessful.com/e/hyauc3pvewkz";

        // when
        Optional<String> result = mediaSourceFetcher.extractHlsUrl(url);

        // then
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("master.m3u8"), "Result is not a valid HLS URL. Got: " + result.get());
    }
}
