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

package com.sic.bb.jenkins.plugins.sicci_for_xcode.io;

import hudson.FilePath;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.sic.bb.jenkins.plugins.sicci_for_xcode.cli.XcodebuildCommandCaller;

public class XcodebuildOutputParser {
	private static final Pattern availableSdksPattern = Pattern.compile("^.*(?:-sdk\\s*)(\\S+)\\s*$");
	private static final Pattern parseXcodeBuildListPattern1 = Pattern.compile("^\\s*((?:[^(\\s]+\\s*)+).*$");
	private static final Pattern parseXcodeBuildListPattern2 = Pattern.compile("^\\s*((?:\\S+\\s*\\S+)+)\\s*$");
	
	private static final String BUILD_CONFIGURATION_PARSE_STRING = "Build Configurations";
	private static final String TARGET_PARSE_STRING = "Targets:";
    
    public static String[] getBuildTargets(FilePath workspace) {
    	return parseXcodebuildList(workspace, TARGET_PARSE_STRING, true);
    }
    
    public static String[] getBuildTargetsNoCache(FilePath workspace) {
    	return parseXcodebuildList(workspace, TARGET_PARSE_STRING, false);
    }
	
	public static String[] getBuildConfigurations(FilePath workspace) {
    	return parseXcodebuildList(workspace, BUILD_CONFIGURATION_PARSE_STRING, true);
    }
	
	public static String[] getBuildConfigurationsNoCache(FilePath workspace) {
    	return parseXcodebuildList(workspace, BUILD_CONFIGURATION_PARSE_STRING, false);
    }
    
    private static String[] parseXcodebuildList(FilePath workspace, String arg, boolean useCache) {
		ArrayList<String> items = new ArrayList<String>();
		boolean found = false;
		
		String itemsString;
		
		if(useCache)
			itemsString = XcodebuildCommandCaller.getInstance().getOutput(workspace,"-list");
		else
			itemsString = XcodebuildCommandCaller.getInstance().getOutputNoCache(workspace,"-list");
		
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
    
    public static String[] getAvailableSdks(FilePath workspace) {
		ArrayList<String> sdks = new ArrayList<String>();
		
		String sdksString = XcodebuildCommandCaller.getInstance().getOutput(workspace,"-list");
		
		if(StringUtils.isBlank(sdksString))
			return new String[0];
		
		for(String sdk: sdksString.split("\n")) {
			if(!sdk.contains("-sdk"))
				continue;
			
			sdks.add(availableSdksPattern.matcher(sdk).replaceAll("$1"));
		}
    
		return (String[]) sdks.toArray(new String[sdks.size()]);
    }
}
