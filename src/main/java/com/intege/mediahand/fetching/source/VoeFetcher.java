package com.intege.mediahand.fetching.source;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class VoeFetcher implements MediaSourceFetcher {

    @Override
    public Optional<HlsUrl> extractHlsUrl(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        Elements scripts = document.select("script");

        Pattern hlsPattern = Pattern.compile("'hls':\\s+'(.*?)'");
        Pattern durationPattern = Pattern.compile("duration: (\\d+)");

        String hlsLink = null;
        long duration = 0;
        for (Element script : scripts) {
            if (hlsLink == null) {
                Matcher matcher = hlsPattern.matcher(script.html());
                if (matcher.find()) {
                    hlsLink = matcher.group(1);
                }
            }
            if (duration == 0) {
                Matcher matcher = durationPattern.matcher(script.html());
                if (matcher.find()) {
                    duration = Long.parseLong(matcher.group(1));
                }
            }
            if (hlsLink != null && duration > 0) {
                return Optional.of(new HlsUrl(hlsLink, duration));
            }
        }
        if (hlsLink != null) {
            return Optional.of(new HlsUrl(hlsLink, duration));
        }
        return Optional.empty();
    }

    @Getter
    @AllArgsConstructor
    public static class HlsUrl {
        private String url;
        private long duration;
    }

}
