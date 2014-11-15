package com.alexecollins.linkchecker;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Reporter {
    private long startTime;

    public void started() {
        startTime = System.currentTimeMillis();
    }

    public void finished() {
        System.out.println("finished, took  " + (System.currentTimeMillis() - startTime) + " ms");
    }

    public void brokenLink(URI page, URI target, IOException e) {
        System.err.println("bad link on " + page + " to " + target + ": " + e);
    }

    public void badLinkSyntax(URI page, String target, URISyntaxException e) {
        System.err.println("bad link on " + page + " to " + target + ": " + e);
    }

    public void crawling(URI uri, int complete, int total) {
        System.out.printf("[%4s/%4s] %s\r", complete, total, uri);
    }

    public void error(Exception e) {
        System.err.println(e);
    }
}
