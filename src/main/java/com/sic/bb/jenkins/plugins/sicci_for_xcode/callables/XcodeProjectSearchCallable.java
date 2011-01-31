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

package com.sic.bb.jenkins.plugins.sicci_for_xcode.callables;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.sic.bb.jenkins.plugins.sicci_for_xcode.filefilter.DirectoryFilter;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.filefilter.XcodeProjectDirectoryFilter;

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