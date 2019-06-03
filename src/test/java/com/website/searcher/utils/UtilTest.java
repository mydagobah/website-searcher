package com.website.searcher.utils;

import com.website.searcher.models.MatchResult;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class UtilTest {
    private Util util;

    @Before
    public void init() {
        util = new Util();
    }

    @Test
    public void test_parseUrl_empty() throws Exception {
        BufferedReader br = mock(BufferedReader.class);
        when(br.readLine()).thenReturn(null);

        List<String> result = util.parseUrl(br);

        assertTrue(result.isEmpty());
    }

    @Test
    public void test_parseUrl_skipHeader() throws Exception {
        BufferedReader br = mock(BufferedReader.class);
        when(br.readLine()).thenReturn("header").thenReturn("1,\"facebook.com/\",120").thenReturn(null);

        List<String> result = util.parseUrl(br);

        assertThat(result.size(), is(1));
        assertThat(result.get(0), is("facebook.com"));
    }

    @Test
    public void test_parseUrl_multiLines() throws Exception {
        BufferedReader br = mock(BufferedReader.class);
        when(br.readLine())
                .thenReturn("header")
                .thenReturn("1,\"facebook.com/\",120")
                .thenReturn("1,\"google.com\",120")
                .thenReturn(null);

        List<String> result = util.parseUrl(br);

        assertThat(result.size(), is(2));
        assertThat(result.get(0), is("facebook.com"));
        assertThat(result.get(1), is("google.com"));
    }

    @Test
    public void test_writeLine() throws Exception {
        MatchResult result = new MatchResult("test.com", MatchResult.Result.NOT_MATCH, "error");
        BufferedWriter bw = mock(BufferedWriter.class);
        util.writeLine(bw, result);

        verify(bw, times(1)).write("test.com, NOT_MATCH, error\n");
    }

    @Test
    public void test_partition_empty() {
        List<List<String>> result = util.partition(new ArrayList<>(), 2);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void test_partition_invalidBatchsize() {
        List<List<String>> result = util.partition(Arrays.asList("a", "b", "c"), -2);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void test_partition_partial() {
        List<List<String>> result = util.partition(Arrays.asList("a", "b", "c"), 2);
        assertThat(result.size(), is(2));
        assertThat(result.get(0), is(Arrays.asList("a", "b")));
        assertThat(result.get(1), is(Arrays.asList("c")));
    }

    @Test
    public void test_partition_one() {
        List<List<String>> result = util.partition(Arrays.asList("a", "b", "c"), 3);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void test_partition_full() {
        List<List<String>> result = util.partition(Arrays.asList("a", "b", "c", "d"), 2);
        assertThat(result.size(), is(2));
        assertThat(result.get(0), is(Arrays.asList("a", "b")));
        assertThat(result.get(1), is(Arrays.asList("c", "d")));
    }
}
