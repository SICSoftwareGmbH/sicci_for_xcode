package com.sic.bb.hudson.plugins.xcodeplugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.sic.bb.hudson.plugins.xcodeplugin.util.XcodeFilteredOutputStream;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;

@Extension
public class XcodeConsoleLogFilter extends ConsoleLogFilter {
	@SuppressWarnings("rawtypes")
	@Override
	public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException {
		List<String> toSuppress = new ArrayList<String>();
		XcodeUserNodeProperty property = XcodeUserNodeProperty.getCurrentNodesProperties();
		
		if(property == null)
			return logger;
		
		if(property.getPassword() != null) {
			toSuppress.add(property.getPassword());
			
			return new XcodeFilteredOutputStream(logger, toSuppress);
		} else
			return logger;
	}

}
