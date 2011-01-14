package com.sic.bb.hudson.plugins.xcodeplugin;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

public class XcodeProjectSearchCallable implements FileCallable<ArrayList<String> > {
	private static final long serialVersionUID = 1L;
	
	private final int searchDepth;
	
	public XcodeProjectSearchCallable(int searchDepth) {
		this.searchDepth = searchDepth;
	}

	public ArrayList<String> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {		
		return searchXcodeProjFiles(workspace, this.searchDepth);
	}
	
    private ArrayList<String> searchXcodeProjFiles(File workspace, int searchDepth) throws IOException {
    	ArrayList<String> projectDirs = new ArrayList<String>();
    	
    	if(searchDepth <= 0)
    		return projectDirs;
    	
		String[] projectDirsTemp = workspace.list(new XcodeProjDirFilter());
		
		if(projectDirsTemp != null && projectDirsTemp.length > 0)
			projectDirs.add(workspace.getCanonicalPath() + File.separatorChar);
		
		String[] dirList = workspace.list(new DirFileFilter());
		
		if(dirList != null)
			for(String dir : workspace.list(new DirFileFilter()))
				if(!projectDirs.contains(workspace.getCanonicalPath() + File.separatorChar + dir))
					projectDirs.addAll(searchXcodeProjFiles(new File(workspace,dir), searchDepth - 1));
		
		return projectDirs;
    }
    
    private final class DirFileFilter implements FilenameFilter {
    	public boolean accept(File path, String filename) {
    		return new File(path,filename).isDirectory();
    	}
    }
    
	private final class XcodeProjDirFilter implements FilenameFilter {
		public boolean accept(File path, String filename) {
			File file = new File(path,filename);
			
			return file.isDirectory() && file.getName().endsWith(".xcodeproj");
		}
    }
}