/*
 * SICCI for Xcode - Jenkins Plugin for Xcode projects
 * 
 * Copyright (C) 2011 Benedikt Biallowons, SIC! Software GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.sic.bb.jenkins.plugins.sicci_for_xcode.cli;

import static com.sic.bb.jenkins.plugins.sicci_for_xcode.util.Constants.RETURN_OK;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.StreamTaskListener;

import com.sic.bb.jenkins.plugins.sicci_for_xcode.io.MaskedOutputStream;

public class SecurityCommandCaller {
	public static final String DEFAULT_KEYCHAIN = "/Users/<USERNAME>/Library/Keychains/login.keychain";
	public static final String SECURITY_COMMAND = "/usr/bin/security";

	private static SecurityCommandCaller instance;

	public static SecurityCommandCaller getInstance() {
		if (instance == null)
			instance = new SecurityCommandCaller();

		return instance;
	}

	public boolean check(VirtualChannel channel, TaskListener listener) {
		try {
			if (new FilePath(channel, SECURITY_COMMAND).exists())
				return true;
		} catch (Exception e) {
		}

		listener.fatalError(SECURITY_COMMAND + ": "
				+ Messages.SecurityCommandCaller_check_commandNotFound() + "\n");
		return false;
	}

	public boolean unlockKeychain(EnvVars envVars, TaskListener listener, FilePath workspace, String username, String password) {
		MaskedOutputStream outputStream = new MaskedOutputStream(listener.getLogger(), password);
		
		try {
			Launcher launcher = workspace.createLauncher(new StreamTaskListener(outputStream));
			
			int rcode = launcher
					.launch()
					.envs(envVars)
					.pwd(workspace)
					.cmds(SECURITY_COMMAND, "unlock-keychain", "-p", password,
							getKeychain(username)).join();

			if (rcode == RETURN_OK)
				return true;
		} catch (Exception e) {
		}

		listener.fatalError(Messages
				.SecurityCommandCaller_unlockKeychain_keychainNotUnlockable());
		return false;
	}

	public boolean lockKeychain(Launcher launcher, EnvVars envVars, TaskListener listener, FilePath workspace, String username) {
		try {
			int rcode = launcher
					.launch()
					.stdout(listener)
					.envs(envVars)
					.pwd(workspace)
					.cmds(SECURITY_COMMAND, "lock-keychain",
							getKeychain(username)).join();

			if (rcode == RETURN_OK)
				return true;
		} catch (Exception e) {
		}

		listener.fatalError(Messages
				.SecurityCommandCaller_lockKeychain_keychainNotLockable());
		return false;
	}

	public boolean createKeychain(EnvVars envVars, TaskListener listener, FilePath workspace, String username, String password) {
		MaskedOutputStream outputStream = new MaskedOutputStream(listener.getLogger(), password);
		
		try {
			Launcher launcher = workspace.createLauncher(new StreamTaskListener(outputStream));

			int rcode = launcher
					.launch()
					.envs(envVars)
					.pwd(workspace)
					.cmds(SECURITY_COMMAND, "create-keychain", "-p", password,
							getKeychain(username)).join();

			if (rcode == RETURN_OK)
				return true;
		} catch (Exception e) {
		}

		listener.fatalError(Messages
				.SecurityCommandCaller_createKeychain_keychainNotCreated());
		return false;
	}

	private static final String getKeychain(String username) {
		if (username == null)
			return null;

		return DEFAULT_KEYCHAIN.replace("<USERNAME>", username);
	}
}
