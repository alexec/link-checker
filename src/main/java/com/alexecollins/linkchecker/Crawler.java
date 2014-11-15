package com.alexecollins.linkchecker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
public class Crawler {
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );
    private final CompletionService<Void> completionService = new ExecutorCompletionService<Void>(executorService);
	private final Set<Future<Void>> futures = Collections.synchronizedSet(new HashSet<Future<Void>>());
	private final Set<URI> uris = Collections.synchronizedSet(new HashSet<URI>());
	private final Map<URI, Set<URI>> toFrom = Collections.synchronizedMap(new HashMap<URI, Set<URI>>());
	private final CachingConfig cachingConfig;
	private final URI home;
    private final Reporter reporter;
    private int complete = 0;
    private int total = 0;

    public Crawler(CachingConfig cachingConfig, URI home, Reporter reporter) {
        this.cachingConfig = cachingConfig;
        this.home = home;
        this.reporter = reporter;
    }

    private static boolean isCrawlable(final URI uri) {
        return uri.getFragment() == null &&
                !uri.getHost().equals("www.google.com");
    }

    private static String readPage(final InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder buffer = new StringBuilder();
        String l;
        try {
            while ((l = in.readLine()) != null) {
                buffer.append(l).append('\n');
            }
        } finally {
            in.close();
        }

        return buffer.toString();
    }

	public void crawl() throws Exception {

        if (!cachingConfig.getDir().exists()) {
            if (!cachingConfig.getDir().mkdir()) {
                throw new IllegalStateException("failed to create cache dir " + cachingConfig.getDir());
            }
        }
        reporter.started();

        try {
            submit(home, null);

            while (futures.size() > 0) {
                final Future<Void> future = completionService.take();

                try {
                    future.get();
                } catch (Exception e) {
                    reporter.error(e);
                }

                futures.remove(future);
                complete++;
            }

        } finally {
            executorService.shutdown();
            reporter.finished();
        }
    }

    private void crawl(final URI uri, final URI parent) {

        reporter.crawling(uri, complete, total);
        Document doc;
        try {
            doc = Jsoup.parse(getPage(uri), home.toString());
        } catch (IOException e) {
            reporter.brokenLink(parent, uri, e);
            return;
        }

        if (!isOnSite(uri)) {
            return;
        }

        Elements links = doc.select("a[href]");

        for (Element link : links) {
            final String attr = link.attr("abs:href");
            final URI href;
            try {
                href = new URI(attr);
            } catch (URISyntaxException e) {
                reporter.badLinkSyntax(uri, attr, e);
                continue;
            }

            if (!isCrawlable(href)) {
                continue;
            }

            synchronized (toFrom) {
                if (!toFrom.containsKey(href)) {
                    toFrom.put(href, new HashSet<URI>());
                }

                toFrom.get(href).add(uri);
            }

            submit(href, uri);
        }
    }

    private void submit(final URI uri, final URI parent) {

        synchronized (uris) {
            if (uris.contains(uri)) {
                return;
            }

            uris.add(uri);
        }

        synchronized (futures) {
            total++;
            futures.add(completionService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    crawl(uri, parent);
                    return null;
                }
            }));
        }
    }

	private boolean isOnSite(final URI href) {
        return home.getHost().equals(href.getHost());
    }

    private String getPage(final URI uri) throws IOException {
        File f = new File(cachingConfig.getDir(), URLEncoder.encode(uri.toString(), "UTF-8"));

        final String page;
        if (!f.exists() || f.lastModified() < System.currentTimeMillis() - cachingConfig.getTime()) {

            // get URI
            final URLConnection connection = uri.toURL().openConnection();
            connection.connect();

            page = readPage(connection.getInputStream());

            PrintWriter out = new PrintWriter(new FileWriter(f));
            try {
                out.write(page);
            } finally {
                out.close();
            }
        } else {
            page = readPage(new FileInputStream(f));
        }

        return page;
    }
}
