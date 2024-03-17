package com.intege.mediahand.fetching.source;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class VoeFetcher implements MediaSourceFetcher {

    @Override
    public Optional<String> extractHlsUrl(String url) throws IOException {
            Document document = Jsoup.connect(url).get();
            Elements scripts = document.select("script");

            Pattern pattern = Pattern.compile("'hls':\\s+'(.*?)'");
            for (Element script : scripts) {
                Matcher matcher = pattern.matcher(script.html());
                if (matcher.find()) {
                    return Optional.of(matcher.group(1));
                }
            }
        return Optional.empty();
    }

}
