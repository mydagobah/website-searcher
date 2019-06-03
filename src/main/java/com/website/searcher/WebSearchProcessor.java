package com.website.searcher;

import com.website.searcher.models.MatchResult;
import com.website.searcher.utils.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class WebSearchProcessor {
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String HTTP_PROTOCOL = "http://";

    private final Util util;

    public WebSearchProcessor(Util util) {
        this.util = util;
    }

    /**
     * Match page content from each url against given criteria in parallel
     * @param urls - a list of urls to process
     * @param criteria - a predicate to determine if a page matches or not
     * @return - a list of {@link MatchResult}
     */
    public List<MatchResult> process(List<String> urls, Predicate<String> criteria) {
        List<CompletableFuture<MatchResult>> futures = urls.stream()
            .map(urlStr -> CompletableFuture.supplyAsync(() -> processUrl(urlStr, criteria)))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(future -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()))
                .join();
    }

    /**
     * try both protocols since some websites only support http
     *
     * @param urlStr - a url without protocol
     * @param criteria - a predicate to determine if a page matches or not
     * @return - {@link MatchResult}
     */
    private MatchResult processUrl(String urlStr, Predicate<String> criteria) {
        try {
            URL url = new URL(HTTPS_PROTOCOL + urlStr);
            if (matchUrl(url, criteria)) {
                return new MatchResult(urlStr, MatchResult.Result.MATCH);
            }
        } catch (Exception e) {
            try {
                URL url = new URL(HTTP_PROTOCOL + urlStr);
                if (matchUrl(url, criteria)) {
                    return new MatchResult(urlStr, MatchResult.Result.MATCH);
                }
            } catch (Exception ee) {
                String errorMessage = ee.getMessage() != null ? ee.getMessage() :
                        (e.getMessage() != null ? e.getMessage() : "UNKNOWN");
                System.out.println(String.format("Exception searching url %s: %s", urlStr, errorMessage));
                return new MatchResult(urlStr, MatchResult.Result.EXCEPTION, errorMessage);
            }
        }

        return new MatchResult(urlStr, MatchResult.Result.NOT_MATCH);
    }

    private boolean matchUrl(URL url, Predicate<String> criteria) throws IOException {
        HttpURLConnection conn = util.connectUrl(url);

        try (BufferedReader br = util.convertInputStream(conn.getContentEncoding(), conn.getInputStream())) {
            String line;
            while ((line = br.readLine()) != null) {
                if (criteria.test(line)) {
                    return true;
                }
            }
        } finally {
            conn.disconnect();
        }
        return false;
    }

    public List<String> parseUrls(String sourceUrl) throws IOException {
        URL url = new URL(sourceUrl);
        HttpURLConnection conn = util.connectUrl(url);

        try (BufferedReader br = util.convertInputStream(conn.getContentEncoding(), conn.getInputStream())) {
            return util.parseUrl(br);
        } finally {
            conn.disconnect();
        }
    }
}
