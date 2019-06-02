package com.website.searcher.utils;

import com.website.searcher.models.MatchResult;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class FileUtilTest {
    private FileUtil fileUtil;

    @Before
    public void init() {
        fileUtil = new FileUtil();
    }

    @Test
    public void test_parseUrl_empty() throws Exception {
        BufferedReader br = mock(BufferedReader.class);
        when(br.readLine()).thenReturn(null);

        List<String> result = fileUtil.parseUrl(br);

        assertTrue(result.isEmpty());
    }

    @Test
    public void test_parseUrl_skipHeader() throws Exception {
        BufferedReader br = mock(BufferedReader.class);
        when(br.readLine()).thenReturn("header").thenReturn("1,\"facebook.com/\",120").thenReturn(null);

        List<String> result = fileUtil.parseUrl(br);

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

        List<String> result = fileUtil.parseUrl(br);

        assertThat(result.size(), is(2));
        assertThat(result.get(0), is("facebook.com"));
        assertThat(result.get(1), is("google.com"));
    }

    @Test
    public void test_writeLine() throws Exception {
        MatchResult result = new MatchResult("test.com", MatchResult.Result.NOT_MATCH, "error");
        BufferedWriter bw = mock(BufferedWriter.class);
        fileUtil.writeLine(bw, result);

        verify(bw, times(1)).write("test.com, NOT_MATCH, error\n");
    }
}
