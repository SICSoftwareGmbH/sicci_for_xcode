package com.sic.bb.hudson.plugins.xcodeplugin.callables;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;

public class CheckXcodeInstallationCallable implements FileCallable<Boolean> {
	private static final long serialVersionUID = 1L;
	
	private final BuildListener listener;
	private final String username;
	private final String password;
	
	public CheckXcodeInstallationCallable(BuildListener listener, String username, String password) {
		this.listener = listener;
		this.username = username;
		this.password = password;
	}

	public Boolean invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {

		return null;
	}
}
