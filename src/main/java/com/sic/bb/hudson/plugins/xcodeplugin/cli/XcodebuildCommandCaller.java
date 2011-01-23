package com.sic.bb.hudson.plugins.xcodeplugin.cli;


import static com.sic.bb.hudson.plugins.xcodeplugin.util.Constants.RETURN_OK;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class XcodebuildCommandCaller {
	public static final String XCODEBUILD_COMMAND = "/usr/bin/xcodebuild";

	private static XcodebuildCommandCaller instance;

	private String xcodebuildOutputTemp;
	private String workspaceTemp;

	public static XcodebuildCommandCaller getInstance() {
		if (instance == null)
			instance = new XcodebuildCommandCaller();

		return instance;
	}

	public void setWorkspaceTemp(String workspace) {
		if (this.workspaceTemp != null
				&& this.workspaceTemp.contains(workspace))
			this.workspaceTemp = null;
	}
	
	public boolean check(VirtualChannel channel, TaskListener listener) {
		try {
			if (new FilePath(channel, XCODEBUILD_COMMAND).exists())
				return true;
		} catch (Exception e) {
		}

		listener.fatalError(XCODEBUILD_COMMAND + ": "
				+ Messages.XcodebuildCommandCaller_check_commandNotFound());
		return false;
	}

	public boolean build(EnvVars envVars, TaskListener listener, FilePath workspace, List<String> args) {
		args.add("build");
		
		try {
			// TODO
			Launcher launcher = workspace.createLauncher(listener);
			return callReturnBoolean(launcher, envVars, listener, workspace, args);
		} catch (Exception e) {
		}

		return false;
	}

	public boolean clean(EnvVars envVars, TaskListener listener, FilePath workspace, List<String> args) {
		args.add("clean");
		
		try {
			// TODO
			Launcher launcher = workspace.createLauncher(listener);
			return callReturnBoolean(launcher, envVars, listener, workspace, args);
		} catch (Exception e) {
		}

		return false;
	}

	public String callReturnString(FilePath workspace, String arg) {
		// TODO workspace.toString() will be called (deprecated)
		if (this.workspaceTemp != null
				&& this.workspaceTemp.equals(workspace + arg))
			return this.xcodebuildOutputTemp;
		else
			this.workspaceTemp = workspace + arg;

		ByteArrayOutputStream stdout = new ByteArrayOutputStream();

		try {
			Launcher launcher = workspace
					.createLauncher(new StreamTaskListener(stdout));

			launcher.launch().stdout(stdout).pwd(workspace)
					.cmds(XCODEBUILD_COMMAND, arg).join();

		} catch (Exception e) {
		}

		this.xcodebuildOutputTemp = stdout.toString();

		return this.xcodebuildOutputTemp;
	}

	private boolean callReturnBoolean(Launcher launcher, EnvVars envVars,
			TaskListener listener, FilePath workspace, List<String> args) {
		args.add(0, XCODEBUILD_COMMAND);

		try {
			int rcode = launcher.launch().envs(envVars).stdout(listener)
					.pwd(workspace).cmds(args).join();

			if (rcode == RETURN_OK)
				return true;
		} catch (Exception e) {
		}

		return false;
	}
}
