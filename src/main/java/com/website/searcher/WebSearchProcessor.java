package com.website.searcher;

import com.website.searcher.models.MatchResult;
import com.website.searcher.utils.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

class WebSearchProcessor {
    private static final String HTTPS_PROTOCOL = "https://";
    private static final String HTTP_PROTOCOL = "http://";

    private final Util util;

    public WebSearchProcessor(Util util) {
        this.util = util;
    }

    /**
     * Match each url content against given criteria in parallel
     * @param urls - a list of urls to process
     * @param criteria - a predicate to determine if a page matches or not
     * @param writer - a buffered writer to output results
     * @return - a list of {@link MatchResult}
     */
    public Map<MatchResult.Result, Integer> process(List<String> urls, Predicate<String> criteria, BufferedWriter writer,
                                                    int concurrentHttpLimit) {
        ConcurrentHashMap<MatchResult.Result, Integer> report = new ConcurrentHashMap<>();
        AtomicInteger counter = new AtomicInteger();

        List<Thread> threads = new ArrayList<>();

        for (String url : urls) {

            while (counter.get() >= concurrentHttpLimit) {
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Thread task = new Thread(new Task(url, criteria, counter, writer, report));
            task.start();
            threads.add(task);
        }

        // wait for all writes to finish before closing
        for (Thread task : threads) {
            try {
                task.join();
            } catch (InterruptedException e) {
                System.out.println("Exception while waiting on task");
            }
            if (task.isAlive()) {
                task.interrupt();
            }
        }

        return report;
    }

    /**
     * try both protocols since some websites only support http
     *
     * @param urlStr - a url without protocol
     * @return - {@link MatchResult}
     */
    private MatchResult processUrl(String urlStr, Predicate<String> criteria, AtomicInteger counter) {
        System.out.println(String.format("\tprocessing %s", urlStr));
        try {
            URL url = new URL(HTTPS_PROTOCOL + urlStr);
            if (matchUrl(url, criteria, counter)) {
                return new MatchResult(urlStr, MatchResult.Result.MATCH);
            }
        } catch (Exception e) {
            try {
                URL url = new URL(HTTP_PROTOCOL + urlStr);
                if (matchUrl(url, criteria, counter)) {
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

    private boolean matchUrl(URL url, Predicate<String> criteria, AtomicInteger counter) throws IOException {
        HttpURLConnection conn = util.connectUrl(url);
        counter.incrementAndGet();

        try (BufferedReader br = util.convertInputStream(conn.getContentEncoding(), conn.getInputStream())) {
            String line;
            while ((line = br.readLine()) != null) {
                if (criteria.test(line)) {
                    return true;
                }
            }
        } finally {
            conn.disconnect();
            counter.decrementAndGet();
            synchronized (this) {
                notify();
            }
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

    class Task implements Runnable {
        String url;
        Predicate<String> criteria;
        AtomicInteger counter;
        BufferedWriter writer;
        ConcurrentHashMap<MatchResult.Result, Integer> report;

        Task (String url,
              Predicate<String> criteria,
              AtomicInteger counter,
              BufferedWriter writer,
              ConcurrentHashMap<MatchResult.Result, Integer> report) {
            this.url = url;
            this.criteria = criteria;
            this.counter = counter;
            this.writer = writer;
            this.report = report;
        }

        @Override
        public void run() {
            MatchResult result = processUrl(url, criteria, counter);

            report.put(result.getResult(), report.getOrDefault(result.getResult(), 0) + 1);

            try {
                util.writeLine(writer, result);
            } catch (IOException e) {
                System.out.println(String.format("exception writing result for url %s with result %s. error=%s",
                        url, result.getResult().name(), e.getMessage()));
            }
        }
    }
}
