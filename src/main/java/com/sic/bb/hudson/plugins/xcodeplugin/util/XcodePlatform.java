package com.sic.bb.hudson.plugins.xcodeplugin.util;


public final class XcodePlatform {
	public static final String[] Platforms = new String[] {"Mac OS X", "iOS", "iOS Simulator"};

	public static String getProjectBuildDirName(String xcodeProjectType, String configurationName) {
		if(xcodeProjectType == null || configurationName == null)
			return null;
		
		if(xcodeProjectType.equals(Platforms[0]))
			return configurationName;
		else if(xcodeProjectType.equals(Platforms[1]))
			return configurationName + "-iphoneos";
		else if(xcodeProjectType.equals(Platforms[2]))
			return configurationName + "-iphonesimulator";
		else
			return null;
	}
}
