package com.website.searcher;

import com.website.searcher.models.MatchResult;
import com.website.searcher.utils.Util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Svc {
    private static final String INPUT_URL = "https://s3.amazonaws.com/fieldlens-public/urls.txt";
    private static final String OUTPUT_FILE = "results.txt";
    private static final int CONCURRENT_HTTP_LIMIT = 20;

    public static void main(String[] args) {
        String search = args.length > 0 ? args[0] : ".*about.*";
        Pattern pattern = Pattern.compile(search, Pattern.CASE_INSENSITIVE);
        Predicate<String> criteria = content -> pattern.matcher(content).matches();

        Util util = new Util();
        WebSearchProcessor processor = new WebSearchProcessor(util);

        List<String> urls;
        try {
            urls = processor.parseUrls(INPUT_URL);
        } catch (IOException e) {
            System.out.println(String.format("Exception reading urls from %s: %s", INPUT_URL, e.getMessage()));
            return;
        }

        System.out.println(String.format("Start website search. Search term: \"%s\", total urls: %d", search, urls.size()));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE))) {
            writer.write("url, match_result, error_message\n");

            Map<MatchResult.Result, Integer> report = processor.process(urls, criteria, writer, CONCURRENT_HTTP_LIMIT);

            writer.flush();
            printReport(report);
        } catch (IOException e) {
            System.out.println(String.format("Exception writing into output file %s: %s", OUTPUT_FILE, e.getMessage()));
        }
    }

    private static void printReport(Map<MatchResult.Result, Integer> report) {
        System.out.println("##### Summary #####");
        for (MatchResult.Result result : report.keySet()) {
            System.out.println(String.format("Total %s: %d", result.name(), report.get(result)));
        }
    }
}
