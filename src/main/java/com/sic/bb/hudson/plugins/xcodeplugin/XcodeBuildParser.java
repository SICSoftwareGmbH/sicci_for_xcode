package com.sic.bb.hudson.plugins.xcodeplugin;

import hudson.FilePath;
import hudson.Launcher;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public final class XcodebuildParser {
	private final static Pattern availableSdksPattern = Pattern.compile("^.*(?:-sdk\\s*)(\\S+)\\s*$");
	private final static Pattern parseXcodeBuildListPattern1 = Pattern.compile("^\\s*((?:[^(\\s]+\\s*)+).*$");
	private final static Pattern parseXcodeBuildListPattern2 = Pattern.compile("^\\s*((?:\\S+\\s*\\S+)+)\\s*$");
	
	private String xcodebuild;
	private String xcodebuildOutputTemp;
	private String workspaceTemp;
	
	public XcodebuildParser(String xcodebuild) {
		this.xcodebuild = xcodebuild;
	}
	
	public void setXcodebuild(String xcodebuild) {
		this.xcodebuild = xcodebuild;
	}
	
	public void setWorkspaceTemp(String workspace) {
		if(this.workspaceTemp != null && this.workspaceTemp.contains(workspace))
			this.workspaceTemp = null;
	}
    
	public String[] getBuildConfigurations(FilePath workspace) {
    	return parseXcodebuildList(workspace, "Build Configurations:");
    }
    
    public String[] getBuildTargets(FilePath workspace) {
    	return parseXcodebuildList(workspace, "Targets:");
    }
    
    public String[] getAvailableSdks(FilePath workspace) {
		ArrayList<String> sdks = new ArrayList<String>();
		
		for(String sdk: callXcodebuild(workspace,"-showsdks").split("\n")) {
			if(!sdk.contains("-sdk"))
				continue;
			
			sdks.add(availableSdksPattern.matcher(sdk).replaceAll("$1"));
		}
    
		return (String[]) sdks.toArray(new String[sdks.size()]);
    }
    
    private String[] parseXcodebuildList(FilePath workspace, String arg) {
		ArrayList<String> items = new ArrayList<String>();
		boolean found = false;
		
		for(String item: callXcodebuild(workspace,"-list").split("\n")) {
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
