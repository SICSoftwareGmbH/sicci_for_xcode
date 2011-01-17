package com.sic.bb.hudson.plugins.xcodeplugin.util;

import hudson.FilePath;
import hudson.Launcher;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.sic.bb.hudson.plugins.xcodeplugin.callables.CheckXcodeInstallationCallable;

public final class XcodebuildParser {
	private static final Pattern availableSdksPattern = Pattern.compile("^.*(?:-sdk\\s*)(\\S+)\\s*$");
	private static final Pattern parseXcodeBuildListPattern1 = Pattern.compile("^\\s*((?:[^(\\s]+\\s*)+).*$");
	private static final Pattern parseXcodeBuildListPattern2 = Pattern.compile("^\\s*((?:\\S+\\s*\\S+)+)\\s*$");
	
	private static final String BUILD_CONFIGURATION_PARSE_STRING = "Build Configurations";
	private static final String TARGET_PARSE_STRING = "Targets:";
	
	private String xcodebuildOutputTemp;
	private String workspaceTemp;
	
	public void setWorkspaceTemp(String workspace) {
		if(this.workspaceTemp != null && this.workspaceTemp.contains(workspace))
			this.workspaceTemp = null;
	}
    
	public String[] getBuildConfigurations(FilePath workspace) {
    	return parseXcodebuildList(workspace, BUILD_CONFIGURATION_PARSE_STRING);
    }
    
    public String[] getBuildTargets(FilePath workspace) {
    	return parseXcodebuildList(workspace, TARGET_PARSE_STRING);
    }
    
    public String[] getAvailableSdks(FilePath workspace) {
		ArrayList<String> sdks = new ArrayList<String>();
		
		String sdksString = callXcodebuild(workspace,"-list");
		
		if(StringUtils.isBlank(sdksString))
			return new String[0];
		
		for(String sdk: sdksString.split("\n")) {
			if(!sdk.contains("-sdk"))
				continue;
			
			sdks.add(availableSdksPattern.matcher(sdk).replaceAll("$1"));
		}
    
		return (String[]) sdks.toArray(new String[sdks.size()]);
    }
    
    private String[] parseXcodebuildList(FilePath workspace, String arg) {
		ArrayList<String> items = new ArrayList<String>();
		boolean found = false;
		
		String itemsString = callXcodebuild(workspace,"-list");
		
		if(StringUtils.isBlank(itemsString))
			return new String[0];
		
		for(String item: itemsString.split("\n")) {
			if(item.contains(arg)) {
				found = true;
				continue;
			}
			
			if(!found) continue;
			if(item.isEmpty()) break;
			
			item = parseXcodeBuildListPattern1.matcher(item).replaceAll("$1");
			items.add(parseXcodeBuildListPattern2.matcher(item).replaceAll("$1"));
		}
    
		return (String[]) items.toArray(new String[items.size()]);
    }
    
    private String callXcodebuild(FilePath workspace, String arg) {
    	// TODO workspace.toString() will be called (deprecated)
    	if(this.workspaceTemp != null && this.workspaceTemp.equals(workspace + arg))
    		return this.xcodebuildOutputTemp;
    	else
    		this.workspaceTemp = workspace + arg;
    	
    	FilePath file = new FilePath(workspace,new String());
    	ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    	
    	try {
    		Launcher launcher = file.createLauncher(new StreamTaskListener(new ByteArrayOutputStream()));
    		
    		if(launcher.isUnix())
    			launcher.launch().stdout(stdout).pwd(workspace).cmds(CheckXcodeInstallationCallable.XCODEBUILD_COMMAND, arg).join();
		} catch (Exception e) {
			// TODO
		}
		
		this.xcodebuildOutputTemp = stdout.toString();
		
		return this.xcodebuildOutputTemp;
    }
}
