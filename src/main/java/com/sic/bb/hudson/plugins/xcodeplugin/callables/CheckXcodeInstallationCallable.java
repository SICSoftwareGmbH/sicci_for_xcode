package com.sic.bb.hudson.plugins.xcodeplugin.callables;

import java.io.File;
import java.io.IOException;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class CheckXcodeInstallationCallable implements FileCallable<Boolean> {
	private static final long serialVersionUID = 1L;
	
	private static final String xcodebuild = "/usr/bin/xcodebuild";
	private static final String security = "/usr/bin/security";
	private static final String keychain = "/Users/<USERNAME>/Library/Keychains/login.keychain";
	
	private final String username;
	private final String password;
	
	public CheckXcodeInstallationCallable(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public Boolean invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
		return null;
	}

	public static final String getKeychain(String username) {
		return keychain.replace("<USERNAME>", username);
	}
}
