package com.intege.mediahand.fetching;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import com.intege.mediahand.utils.FetchingUtil;

public class AniworldFetcher implements SourceFetcher {

    @Override
    public List<URL> extractVoeUrl(final URL url) throws IOException {
        Document document = Jsoup.connect(url.toExternalForm()).get();
        Elements links = document.select("a");
        List<URL> urls = new ArrayList<>();
        for (Element link : links) {
            Element h4 = link.selectFirst("h4");
            if (h4 != null && h4.text().equals("VOE")) {
                String redirectHref = link.attr("href");
                if (!StringUtils.isEmpty(redirectHref)) {
                    URL aniworldRedirect = new URL(url.getProtocol() + "://" + url.getHost() + "/" + redirectHref);
                    urls.add(FetchingUtil.followRedirects(aniworldRedirect));
                }
            }
        }
        return urls;
    }

    @Override
    public List<URL> extractEpisodes(final URL url) throws IOException {
        Document document = Jsoup.connect(url.toExternalForm()).get();
        Elements links = document.select("tr td.seasonEpisodeTitle:has(a[href]) a[href]");

        List<URL> hrefList = new ArrayList<>();
        String urlPath = url.getPath() + "/";
        for (Element link : links) {
            String href = link.attr("href");
            if (href.startsWith(urlPath)) {
                hrefList.add(new URL(url.getProtocol() + "://" + url.getHost() + href));
            }
        }
        return hrefList;
    }

}
