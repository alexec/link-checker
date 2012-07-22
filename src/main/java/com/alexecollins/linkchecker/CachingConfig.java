package com.alexecollins.linkchecker;

import java.io.File;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
public class CachingConfig {
	private final int time;
	private final File dir;

	public CachingConfig(int time, File dir) {
		this.time = time;
		this.dir = dir;
	}


	public int getTime() {
		return time;
	}

	public File getDir() {
		return dir;
	}
}
