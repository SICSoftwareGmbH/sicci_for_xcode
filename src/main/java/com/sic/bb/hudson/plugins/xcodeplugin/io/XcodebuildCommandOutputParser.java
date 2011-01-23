package com.sic.bb.hudson.plugins.xcodeplugin.io;

import hudson.FilePath;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.sic.bb.hudson.plugins.xcodeplugin.cli.XcodebuildCommandCaller;

public final class XcodebuildCommandOutputParser {
	private static final Pattern availableSdksPattern = Pattern.compile("^.*(?:-sdk\\s*)(\\S+)\\s*$");
	private static final Pattern parseXcodeBuildListPattern1 = Pattern.compile("^\\s*((?:[^(\\s]+\\s*)+).*$");
	private static final Pattern parseXcodeBuildListPattern2 = Pattern.compile("^\\s*((?:\\S+\\s*\\S+)+)\\s*$");
	
	private static final String BUILD_CONFIGURATION_PARSE_STRING = "Build Configurations";
	private static final String TARGET_PARSE_STRING = "Targets:";
    
	public static String[] getBuildConfigurations(FilePath workspace) {
    	return parseXcodebuildList(workspace, BUILD_CONFIGURATION_PARSE_STRING);
    }
    
    public static String[] getBuildTargets(FilePath workspace) {
    	return parseXcodebuildList(workspace, TARGET_PARSE_STRING);
    }
    
    public static String[] getAvailableSdks(FilePath workspace) {
		ArrayList<String> sdks = new ArrayList<String>();
		
		String sdksString = XcodebuildCommandCaller.getInstance().callReturnString(workspace,"-list");
		
		if(StringUtils.isBlank(sdksString))
			return new String[0];
		
		for(String sdk: sdksString.split("\n")) {
			if(!sdk.contains("-sdk"))
				continue;
			
			sdks.add(availableSdksPattern.matcher(sdk).replaceAll("$1"));
		}
    
		return (String[]) sdks.toArray(new String[sdks.size()]);
    }
    
    private static String[] parseXcodebuildList(FilePath workspace, String arg) {
		ArrayList<String> items = new ArrayList<String>();
		boolean found = false;
		
		String itemsString = XcodebuildCommandCaller.getInstance().callReturnString(workspace,"-list");
		
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
}
