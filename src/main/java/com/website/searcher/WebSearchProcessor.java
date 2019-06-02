package com.website.searcher;

import com.website.searcher.utils.FileUtil;
import com.website.searcher.utils.UrlUtil;
import com.website.searcher.models.MatchResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class WebSearchProcessor {
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String HTTP_PROTOCOL = "http://";

    private final UrlUtil urlUtil;
    private final FileUtil fileUtil;

    WebSearchProcessor(UrlUtil urlUtil, FileUtil fileUtil) {
        this.urlUtil = urlUtil;
        this.fileUtil = fileUtil;
    }

    Map<MatchResult.Result, Integer> process(List<String> urls, Predicate<String> criteria, BufferedWriter writer) {
        Map<MatchResult.Result, Integer> count = new HashMap<>();

        try {
            List<CompletableFuture<MatchResult>> futures = urls.stream()
                .map(urlStr ->
                    CompletableFuture.supplyAsync(() -> {
                        // try both protocols since some websites only support http
                        try {
                            URL url = new URL(HTTPS_PROTOCOL + urlStr);
                            if (matchRegex(url, criteria)) {
                                return new MatchResult(urlStr, MatchResult.Result.MATCH);
                            }
                        } catch (Exception e) {
                            // ignore here
                        }

                        try {
                            URL url = new URL(HTTP_PROTOCOL + urlStr);
                            if (matchRegex(url, criteria)) {
                                return new MatchResult(urlStr, MatchResult.Result.MATCH);
                            }
                            return new MatchResult(urlStr, MatchResult.Result.NOT_MATCH);
                        } catch (Exception e) {
                            String errorMessage = e.getMessage() != null ? e.getMessage() : "UNKNOWN";
                            System.out.println(String.format("Exception searching url %s: %s", urlStr, errorMessage));
                            return new MatchResult(urlStr, MatchResult.Result.EXCEPTION, errorMessage);
                        }
                    })
                ).collect(Collectors.toList());

            List<MatchResult> batchResult = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(future -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()))
                    .join();

            for (MatchResult result : batchResult) {
                fileUtil.writeLine(writer, result);
                count.put(result.getResult(), count.getOrDefault(result.getResult(), 0) + 1);
            }
        } catch (IOException e) {
            System.out.println(String.format("Exception processing batch urls: %s", e.getMessage()));
        }
        return count;
    }

    List<String> parseUrls(String sourceUrl) throws IOException {
        URL url = new URL(sourceUrl);
        try (BufferedReader br = new BufferedReader(urlUtil.fetchUrlContent(url))) {
            return fileUtil.parseUrl(br);
        }
    }

    private boolean matchRegex(URL url, Predicate<String> criteria) throws IOException {
        try (BufferedReader br = new BufferedReader(urlUtil.fetchUrlContent(url))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (criteria.test(line)) {
                    return true;
                }
            }
            return false;
        }
    }
}
