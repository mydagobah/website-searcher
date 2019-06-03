package com.website.searcher;

import com.website.searcher.models.MatchResult;
import com.website.searcher.utils.Util;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class WebSearchProcessorTest {
    private WebSearchProcessor processor;
    private Util util;

    @Before
    public void init() {
        util = mock(Util.class);
        processor = new WebSearchProcessor(util);
    }

    @Test
    public void test_process_https() throws Exception {
        String encoding = "gzip";
        List<String> urls = Arrays.asList("google.com", "yahoo.com");
        Predicate<String> predicate = mock(Predicate.class) ;

        InputStream inputStream = mock(InputStream.class);
        BufferedReader bufferedReader1 = mock(BufferedReader.class);
        when(bufferedReader1.readLine()).thenReturn("line1").thenReturn(null);
        BufferedReader bufferedReader2 = mock(BufferedReader.class);
        when(bufferedReader2.readLine()).thenReturn("line2").thenReturn(null);

        when(predicate.test("line1")).thenReturn(true);
        when(predicate.test("line2")).thenReturn(false);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getContentEncoding()).thenReturn(encoding);
        when(connection.getInputStream()).thenReturn(inputStream);
        when(util.connectUrl(any(URL.class))).thenReturn(connection).thenReturn(connection);
        when(util.convertInputStream(encoding, inputStream)).thenReturn(bufferedReader1).thenReturn(bufferedReader2);

        List<MatchResult> results = processor.process(urls, predicate);
        assertThat(results.size(), is(2));
        List<MatchResult.Result> matchResults = results.stream().map(MatchResult::getResult).collect(Collectors.toList());
        assertThat(matchResults, hasItems(MatchResult.Result.MATCH, MatchResult.Result.NOT_MATCH));
        verify(connection, times(2)).disconnect();
    }

    @Test
    public void test_process_retryWithHttp() throws Exception {
        String encoding = "gzip";
        List<String> urls = Arrays.asList("google.com");
        Predicate<String> predicate = mock(Predicate.class) ;

        InputStream inputStream = mock(InputStream.class);
        BufferedReader bufferedReader = mock(BufferedReader.class);
        when(bufferedReader.readLine()).thenReturn("line1").thenReturn(null);

        when(predicate.test("line1")).thenReturn(false);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getContentEncoding()).thenReturn(encoding);
        when(connection.getInputStream()).thenReturn(inputStream);
        when(util.connectUrl(any(URL.class))).thenThrow(IOException.class).thenReturn(connection);
        when(util.convertInputStream(encoding, inputStream)).thenReturn(bufferedReader);

        List<MatchResult> results = processor.process(urls, predicate);
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getResult(), is(MatchResult.Result.NOT_MATCH));
        verify(util, times(2)).connectUrl(any());
        verify(connection, times(1)).disconnect();
    }

    @Test
    public void test_process_exception() throws Exception {
        List<String> urls = Arrays.asList("abcde.com");
        Predicate<String> predicate = mock(Predicate.class) ;

        when(util.connectUrl(any(URL.class))).thenThrow(IOException.class).thenThrow(IOException.class);

        List<MatchResult> results = processor.process(urls, predicate);
        assertThat(results.size(), is(1));
        assertThat(results.get(0).getResult(), is(MatchResult.Result.EXCEPTION));
        verify(util, times(2)).connectUrl(any());
    }

    @Test
    public void test_parseUrls() throws Exception {
        String encoding = "gzip";
        List<String> urls = new ArrayList<>();

        InputStream inputStream = mock(InputStream.class);
        BufferedReader bufferedReader = mock(BufferedReader.class);

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getContentEncoding()).thenReturn(encoding);
        when(connection.getInputStream()).thenReturn(inputStream);
        when(util.connectUrl(any(URL.class))).thenReturn(connection);
        when(util.convertInputStream(encoding, inputStream)).thenReturn(bufferedReader);
        when(util.parseUrl(bufferedReader)).thenReturn(urls);

        List<String> result = processor.parseUrls("https://google.com");

        assertThat(result, is(urls));
        verify(connection, times(1)).disconnect();
    }
}
