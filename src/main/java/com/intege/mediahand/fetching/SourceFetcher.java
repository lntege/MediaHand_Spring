package com.intege.mediahand.fetching;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public interface SourceFetcher {
    Optional<URL> extractVoeUrl(URL url) throws IOException;
}
