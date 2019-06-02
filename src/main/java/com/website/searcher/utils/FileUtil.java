package com.website.searcher.utils;

import com.website.searcher.models.MatchResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    private static final String SEP = ",";

    /**
     * assuming standard CSV format:
     * "Rank","URL","Linking Root Domains","External Links","mozRank","mozTrust"
     * 1,"facebook.com/",9616487,1688316928,9.54,9.34
     * 2,"twitter.com/",6454936,2147483647,9.40,9.25
     * ...
     * This method skips the very first row (index row) and only read the second columns (Urls) into a list.
     * It also prepends the default https protocol to the urls.
     * @param br - bufferedReader
     * @return a list of Url Strings
     */
    public List<String> parseUrl(BufferedReader br) throws IOException {
        List<String> urls = new ArrayList<>();

        String line = br.readLine(); // skip the header line

        while ((line = br.readLine()) != null) {
            String[] columns = line.split(SEP);

            if (columns.length > 1) {
                String url = columns[1].trim();
                int trail = url.charAt(url.length()-2) == '/' ? 2: 1;  // strip the surrounding quotes and trailing slash
                urls.add(url.substring(1, url.length()-trail));
            }
        }

        return urls;
    }

    public void writeLine(BufferedWriter writer, MatchResult result) throws IOException {
        writer.write(String.format("%s, %s, %s\n",
                result.getUrl(),
                result.getResult().name(),
                result.getErrorMessage().orElse("")));
    }
}
