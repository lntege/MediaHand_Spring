package com.intege.mediahand.fetching.source;

import java.io.IOException;
import java.util.Optional;

public interface MediaSourceFetcher {
    Optional<String> extractHlsUrl(String url) throws IOException;
}
