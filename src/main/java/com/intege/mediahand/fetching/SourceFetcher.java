package com.intege.mediahand.fetching;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public interface SourceFetcher {
    List<URL> extractVoeUrl(URL url) throws IOException;

    List<URL> extractEpisodes(URL url) throws IOException;
}
