package com.intege.mediahand.fetching;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class AniworldFetcherTest {

    @Test
    void testExtractVoeUrl() throws IOException {
        // given
        AniworldFetcher aniworldFetcher = new AniworldFetcher();
        URL url = new URL("https://aniworld.to/anime/stream/shangri-la-frontier/staffel-1/episode-23");

        // when
        Optional<Object> result = aniworldFetcher.extractVoeUrl(url);

        // then
        assertTrue(result.isPresent(), "No VOE url found");
        assertTrue(result.get().toString().contains("https://vincentincludesuccessful.com"), "Result is not a valid VOE URL. Got: " + result.get());
    }
}
