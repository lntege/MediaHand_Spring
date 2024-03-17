package com.intege.mediahand.fetching;

import java.util.Optional;

public interface MediaSourceFetcher {
    Optional<String> extractHlsUrl(String url);
}
