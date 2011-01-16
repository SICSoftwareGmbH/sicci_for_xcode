package com.sic.bb.hudson.plugins.xcodeplugin.util;

public final class XcodeProjectType {
	public static final String[] ProjectTypes = new String[] {"Mac OS X", "iOS", "iOS Simulator"};

	public static String getProjectBuildDirName(String xcodeProjectType, String configurationName) {
		if(xcodeProjectType.equals(ProjectTypes[0]))
			return configurationName;
		else if(xcodeProjectType.equals(ProjectTypes[1]))
			return configurationName + "-iphoneos";
		else if(xcodeProjectType.equals(ProjectTypes[2]))
			return configurationName + "-iphonesimulator";
		else
			return null;
	}
}
