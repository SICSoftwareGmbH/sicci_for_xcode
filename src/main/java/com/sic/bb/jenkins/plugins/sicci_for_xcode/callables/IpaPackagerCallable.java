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

import com.sic.bb.jenkins.plugins.sicci_for_xcode.filefilter.AppDirectoryFilter;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.filefilter.IpaFileFilter;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.util.ExtendedFile;

public class IpaPackagerCallable implements FileCallable<Boolean> {
	private static final long serialVersionUID = 1L;
	private static final String subfolderName = "Payload";
	
	private final String targetName;
	private final String fileName;
	
	public IpaPackagerCallable(String targetName, String fileName) {
		this.targetName = targetName;
		this.fileName = fileName;
	}

	public Boolean invoke(File buildDir, VirtualChannel channel) throws IOException, InterruptedException {
		String[] apps = buildDir.list(new AppDirectoryFilter());
		
		if(apps != null) {
            for(String app: apps) {
            	if(!app.equals(this.targetName + AppDirectoryFilter.FILE_ENDING))
            		continue;
                
            	File appDir = new File(buildDir,app);
                ExtendedFile ipaFileContents = new ExtendedFile(buildDir,subfolderName);
                File movedAppDir = new File(ipaFileContents,app);
                
                if(ipaFileContents.exists())
                	ipaFileContents.deleteRecursive();
                
                if(!ipaFileContents.mkdirs())
                	return false;
                
                if(!appDir.renameTo(new File(ipaFileContents,app)))
                	return false;
                
                ipaFileContents.zip(new File(buildDir,this.fileName + IpaFileFilter.FILE_ENDING));
                
                if(!movedAppDir.renameTo(appDir))
                	return false;
                
                ipaFileContents.deleteRecursive();
                
                return true;
            }
		}
		
		return false;
	}
}
