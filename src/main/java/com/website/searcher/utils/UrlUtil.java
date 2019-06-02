package com.website.searcher.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class UrlUtil {
    private static final String ACCEPT_ENCODINGS = "gzip, deflate";

    public InputStreamReader fetchUrlContent(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        HttpURLConnection.setFollowRedirects(true);
        conn.setConnectTimeout(10_000); // 10 seconds
        conn.setRequestProperty("Accept-Encoding", ACCEPT_ENCODINGS);
        InputStream is = decodeInputStream(conn.getContentEncoding(), conn.getInputStream());
        return new InputStreamReader(is);
    }

    /**
     * wrap InputStream based on encoding types
     * @param encoding - encoding type
     * @param inputStream - raw input stream
     * @return properly wrapped input stream based on encoding type
     * @throws IOException
     */
    private InputStream decodeInputStream(String encoding, InputStream inputStream) throws IOException {
        InputStream is;

        if ("gzip".equalsIgnoreCase(encoding)) {
            is = new GZIPInputStream(inputStream);
        } else if ("deflate".equalsIgnoreCase(encoding)) {
            is = new InflaterInputStream(inputStream, new Inflater(true));
        } else {
            is = inputStream;
        }
        return is;
    }
}
