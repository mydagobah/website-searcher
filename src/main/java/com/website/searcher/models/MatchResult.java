package com.website.searcher.models;

import java.util.Optional;

public class MatchResult {
    private String url;
    private Result result;
    private Optional<String> errorMessage;

    public MatchResult(String url, Result result) {
        this.url = url;
        this.result = result;
        this.errorMessage = Optional.empty();
    }

    public MatchResult(String url, Result result, String errorMessage) {
        this.url = url;
        this.result = result;
        this.errorMessage = Optional.of(errorMessage);
    }

    public enum Result {
        MATCH,
        NOT_MATCH,
        EXCEPTION
    }

    public String getUrl() {
        return url;
    }

    public Result getResult() {
        return result;
    }

    public Optional<String> getErrorMessage() {
        return errorMessage;
    }
}
