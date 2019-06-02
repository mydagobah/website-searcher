package com.website.searcher;

import com.website.searcher.models.MatchResult;
import com.website.searcher.utils.FileUtil;
import com.website.searcher.utils.UrlUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Svc {
    private static final String INPUT_URL = "https://s3.amazonaws.com/fieldlens-public/urls.txt";
    private static final String OUTPUT_FILE = "results.txt";
    private static final int BATCH_SIZE = 20;

    public static void main(String[] args) {
        String expr = ".*about.*";
        Pattern pattern = Pattern.compile(expr, Pattern.CASE_INSENSITIVE);
        Predicate<String> criteria = content -> pattern.matcher(content).matches();

        FileUtil fileUtil = new FileUtil();
        UrlUtil urlUtil = new UrlUtil();
        WebSearchProcessor processor = new WebSearchProcessor(urlUtil, fileUtil);

        List<String> urls;
        try {
            urls = processor.parseUrls(INPUT_URL);
        } catch (IOException e) {
            System.out.println(String.format("Exception reading urls from %s: %s", INPUT_URL, e.getMessage()));
            return;
        }
        List<List<String>> batches = partition(urls);

        System.out.println(String.format("Start website search. Search term: \"%s\", total urls: %d", expr, urls.size()));

        Map<MatchResult.Result, Integer> report = new HashMap<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE))) {
            writer.write("url, result, error_message\n");

            for (int i = 0; i < batches.size(); i++) {
                System.out.println("Processing batch " + (i + 1));

                Map<MatchResult.Result, Integer> summary = processor.process(batches.get(i), criteria, writer);

                for (MatchResult.Result key : summary.keySet()) {
                    report.put(key, report.getOrDefault(key, 0) + summary.get(key));
                }
            }
            writer.flush();
            printReport(report);
        } catch (IOException e) {
            System.out.println(String.format("Exception opening output file %s: %s", OUTPUT_FILE, e.getMessage()));
        }
    }

    private static List<List<String>> partition(List<String> urls) {
        List<List<String>> batches = new ArrayList<>();

        for (int i = 0; i < urls.size();) {
            List<String> batch = new ArrayList<>();
            for (int j = 0; j < BATCH_SIZE && i < urls.size(); ++j, ++i) {
                batch.add(urls.get(i));
            }
            batches.add(batch);
        }
        return batches;
    }

    private static void printReport(Map<MatchResult.Result, Integer> report) {
        System.out.println("##### Summary #####");
        for (MatchResult.Result result : report.keySet()) {
            System.out.println(String.format("Total %s: %d", result.name(), report.get(result)));
        }
    }
}
