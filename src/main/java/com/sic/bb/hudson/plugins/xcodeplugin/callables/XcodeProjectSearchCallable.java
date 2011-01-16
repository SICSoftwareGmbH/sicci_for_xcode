package com.sic.bb.hudson.plugins.xcodeplugin.callables;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.sic.bb.hudson.plugins.xcodeplugin.filefilter.DirectoryFilter;
import com.sic.bb.hudson.plugins.xcodeplugin.filefilter.XcodeProjectDirectoryFilter;

public class XcodeProjectSearchCallable implements FileCallable<ArrayList<String> > {
	private static final long serialVersionUID = 1L;
	
	private final int searchDepth;
	
	public XcodeProjectSearchCallable(int searchDepth) {
		this.searchDepth = searchDepth;
	}

	public ArrayList<String> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {		
		return searchXcodeProjFiles(workspace, this.searchDepth);
	}
	
    private static ArrayList<String> searchXcodeProjFiles(File workspace, int searchDepth) throws IOException {
    	ArrayList<String> projectDirs = new ArrayList<String>();
    	
    	if(searchDepth <= 0)
    		return projectDirs;
    	
		String[] projectDirsTemp = workspace.list(new XcodeProjectDirectoryFilter());
		
		if(projectDirsTemp != null && projectDirsTemp.length > 0)
			projectDirs.add(workspace.getCanonicalPath() + File.separatorChar);
		
		String[] dirList = workspace.list(new DirectoryFilter());
		
		if(dirList != null)
			for(String dir : workspace.list(new DirectoryFilter()))
				if(!projectDirs.contains(workspace.getCanonicalPath() + File.separatorChar + dir))
					projectDirs.addAll(searchXcodeProjFiles(new File(workspace,dir), searchDepth - 1));
		
		return projectDirs;
    }
}