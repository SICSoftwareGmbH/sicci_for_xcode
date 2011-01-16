package com.sic.bb.hudson.plugins.xcodeplugin.callables;

import java.io.File;
import java.io.IOException;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

public class CheckXcodeInstallationCallable implements FileCallable<Boolean> {
	private static final long serialVersionUID = 1L;
	
	public static final String XCODEBUILD_COMMAND = "/usr/bin/xcodebuild";
	public static final String SECURITY_COMMAND = "/usr/bin/security";
	
	private static final String DEFAULT_KEYCHAIN = "/Users/<USERNAME>/Library/Keychains/login.keychain";
	
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

	public static final String getKeychain(String username) {
		if(username == null)
			return null;
		
		return DEFAULT_KEYCHAIN.replace("<USERNAME>", username);
	}
}
