package com.intege.mediahand.fetching;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

class AniworldFetcherTest {

    @Test
    void testExtractVoeUrl() throws IOException {
        // given
        AniworldFetcher aniworldFetcher = new AniworldFetcher();
        URL url = new URL("https://aniworld.to/anime/stream/shangri-la-frontier/staffel-1/episode-23");

        // when
        List<URL> result = aniworldFetcher.extractVoeUrl(url);

        // then
        assertFalse(result.isEmpty(), "No VOE url found");
        assertTrue(result.get(0).toString().contains("https://vincentincludesuccessful.com"), "Result is not a valid VOE URL. Got: " + result.get(0));
    }
}
