package com.intege.mediahand.fetching.source;

import java.io.IOException;
import java.util.Optional;

public interface MediaSourceFetcher {
    Optional<VoeFetcher.HlsUrl> extractHlsUrl(String url) throws IOException;
}
