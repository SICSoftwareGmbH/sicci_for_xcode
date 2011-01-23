package com.sic.bb.hudson.plugins.xcodeplugin.util;

import java.util.ArrayList;
import java.util.List;


public enum XcodePlatform {
	MAC_OS_X("Mac OS X"),
	IOS("iOS")
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
	IOS_SIMULATOR("iOS Simulator")
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
	
	private final String xcodePlatform;
	
	XcodePlatform(String xcodePlatform) {
		this.xcodePlatform = xcodePlatform;
	}
	
	public String getXcodePlatform() {
		return this.xcodePlatform;
	}
	
	public List<String> getProjectBuildDirNames(String configurationName) {
		if(configurationName == null)
			return null;
		
		List<String> projectBuildDirNames = new ArrayList<String>();
		projectBuildDirNames.add(configurationName);
		
		return projectBuildDirNames;
	}
	
	public static XcodePlatform fromString(String xcodePlatform) {
		if(xcodePlatform != null)
			for(XcodePlatform platForm: XcodePlatform.values())
				if(xcodePlatform.equals(platForm.getXcodePlatform()))
					return platForm;
		
		return null;
	}
	
	public static String[] getXcodePlatforms() {
       	List<String> xcodePlatforms = new ArrayList<String>();
    	
    	for(XcodePlatform xcodePlatform: XcodePlatform.values())
    		xcodePlatforms.add(xcodePlatform.getXcodePlatform());
    	
    	return (String[]) xcodePlatforms.toArray(new String[xcodePlatforms.size()]);
	}
}