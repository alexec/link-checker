package com.alexecollins.linkchecker;

import java.io.File;
import java.net.URI;
import java.util.UUID;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
public class LinkChecker {

	public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("I need a URL to crawl");
            System.exit(1);
        }

		CachingConfig cachingConfig = new CachingConfig(
				Integer.parseInt(System.getProperty("crawler.cacheTime", "300000")),
				new File(System.getProperty("java.io.tmpdir"), "link-checker-page-cache" + (
						Boolean.parseBoolean(System.getProperty("cache.static", "false")) ? "" : UUID.randomUUID()
				))
		);

        new Crawler(cachingConfig, new URI(args[0]), new Reporter()).crawl();

	}
}
