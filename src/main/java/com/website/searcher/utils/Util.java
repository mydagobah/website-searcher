package com.website.searcher.utils;

import com.website.searcher.models.MatchResult;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Util {
    private static final String ACCEPT_ENCODINGS = "gzip, deflate";
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

    public List<List<String>> partition(List<String> urls, int batchSize) {
        List<List<String>> batches = new ArrayList<>();

        if (batchSize > 0) {
            for (int i = 0; i < urls.size();) {
                List<String> batch = new ArrayList<>();
                for (int j = 0; j < batchSize && i < urls.size(); ++j, ++i) {
                    batch.add(urls.get(i));
                }
                batches.add(batch);
            }
        }

        return batches;
    }

    /**
     * wrap InputStream based on encoding types
     * @param encoding - encoding type
     * @param inputStream - raw input stream
     * @return {@link BufferedReader} after properly wrapped input stream based on encoding type
     * @throws IOException
     */
    public BufferedReader convertInputStream(String encoding, InputStream inputStream) throws IOException {
        InputStream is;

        if ("gzip".equalsIgnoreCase(encoding)) {
            is = new GZIPInputStream(inputStream);
        } else if ("deflate".equalsIgnoreCase(encoding)) {
            is = new InflaterInputStream(inputStream, new Inflater(true));
        } else {
            is = inputStream;
        }
        return new BufferedReader(new InputStreamReader(is));
    }

    public HttpURLConnection connectUrl(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(10_000); // 10 seconds
        conn.setRequestProperty("Accept-Encoding", ACCEPT_ENCODINGS);
        return conn;
    }
}
