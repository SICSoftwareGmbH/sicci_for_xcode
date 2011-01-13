package com.sic.bb.hudson.plugins.xcodeplugin;

import hudson.FilePath;
import hudson.Launcher;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class XcodeBuildParser {
	private final static Pattern availableSdksPattern = Pattern.compile("^.*(?:-sdk\\s*)(\\S+)\\s*$");
	private final static Pattern parseXcodeBuildListPattern1 = Pattern.compile("^\\s*((?:[^(\\s]+\\s*)+).*$");
	private final static Pattern parseXcodeBuildListPattern2 = Pattern.compile("^\\s*((?:\\S+\\s*\\S+)+)\\s*$");
	
	private String xcodebuildOutputTemp, workspaceTemp;
	
	public String[] getBuildConfigurations(String workspace) {
    	return parseXcodebuildList(workspace, "Build Configurations:");
    }
    
    public String[] getBuildTargets(String workspace) {
    	return parseXcodebuildList(workspace, "Targets:");
    }
    
    public String[] availableSdks(String workspace) {
		ArrayList<String> sdks = new ArrayList<String>();
		
		for(String sdk: callXcodebuild(workspace,"-showsdks").toString().split("\n")) {
			if(!sdk.contains("-sdk"))
				continue;
			
			sdks.add(availableSdksPattern.matcher(sdk).replaceAll("$1"));
		}
    
		return (String[]) sdks.toArray(new String[sdks.size()]);
    }
    
    private String[] parseXcodebuildList(String workspace, String arg) {
		ArrayList<String> items = new ArrayList<String>();
		boolean found = false;
		
		for(String item: callXcodebuild(workspace,"-list").toString().split("\n")) {
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
    
    private String callXcodebuild(String workspace, String arg) {
    	if(this.workspaceTemp != null && this.workspaceTemp.equals(workspace + arg))
    		return this.xcodebuildOutputTemp;
    	else
    		this.workspaceTemp = workspace + arg;
    	
    	FilePath file = new FilePath(new File(this.xcodebuild));
    	ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    	
    	try {
    		Launcher launcher = file.createLauncher(new StreamTaskListener(new ByteArrayOutputStream()));
    		launcher.launch().stdout(stdout).pwd(workspace).cmds(this.xcodebuild, arg).join();
		} catch (IOException e) {
			// TODO
			return "IOException: " + e.getMessage();
		} catch (InterruptedException e) {
			// TODO
			return "InterruptedException: " + e.getMessage();
		}
		
		this.xcodebuildOutputTemp = stdout.toString();
		
		return this.xcodebuildOutputTemp;
    }
}
