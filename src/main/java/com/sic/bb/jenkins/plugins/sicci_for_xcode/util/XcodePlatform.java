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

package com.sic.bb.jenkins.plugins.sicci_for_xcode.util;

import java.util.ArrayList;
import java.util.List;

public enum XcodePlatform {
	MAC_OS_X("Mac OS X","macosx")
	{
		public List<String> getProjectBuildDirNames(String configurationName) {
			if(configurationName == null)
				return null;
			
			List<String> projectBuildDirNames = new ArrayList<String>();
			projectBuildDirNames.add(configurationName);
			
			return projectBuildDirNames;
		}
	},
	IOS("iOS","iphoneos")
	{
		public List<String> getProjectBuildDirNames(String configurationName) {
			if(configurationName == null)
				return new ArrayList<String>();
			
			List<String> projectBuildDirNames = new ArrayList<String>();
			projectBuildDirNames.add(configurationName + "-iphoneos");
			//projectBuildDirNames.add(configurationName + "-ios");
			
			return projectBuildDirNames;
		}
	},
	IOS_SIMULATOR("iOS Simulator","iphonesimulator")
	{
		public List<String> getProjectBuildDirNames(String configurationName) {
			if(configurationName == null)
				return new ArrayList<String>();
			
			List<String> projectBuildDirNames = new ArrayList<String>();
			projectBuildDirNames.add(configurationName + "-iphonesimulator");
			//projectBuildDirNames.add(configurationName + "-iosimulator");
			
			return projectBuildDirNames;
		}
	};
	
	private final String xcodePlatformName;
	private final String xcodePlatformSdkName;
	
	XcodePlatform(String xcodePlatformName, String xcodePlatformSdkName) {
		this.xcodePlatformName = xcodePlatformName;
		this.xcodePlatformSdkName = xcodePlatformSdkName;
	}
	
	public String getXcodePlatformName() {
		return this.xcodePlatformName;
	}
	
	public String getXcodePlatformSdkName() {
		return this.xcodePlatformSdkName;
	}
	
	public List<String> getProjectBuildDirNames(String configurationName) {
		return new ArrayList<String>();
	}
	
	public static XcodePlatform fromString(String xcodePlatform) {
		if(xcodePlatform != null)
			for(XcodePlatform platForm: XcodePlatform.values())
				if(xcodePlatform.equals(platForm.getXcodePlatformName()))
					return platForm;
		
		return null;
	}
	
	public static String[] getXcodePlatformNames() {
       	List<String> xcodePlatforms = new ArrayList<String>();
    	
    	for(XcodePlatform xcodePlatform: XcodePlatform.values())
    		xcodePlatforms.add(xcodePlatform.getXcodePlatformName());
    	
    	return (String[]) xcodePlatforms.toArray(new String[xcodePlatforms.size()]);
	}
}