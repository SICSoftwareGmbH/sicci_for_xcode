package com.sic.bb.hudson.plugins.xcodeplugin.callables;

import java.io.File;
import java.io.IOException;

import com.sic.bb.hudson.plugins.xcodeplugin.filefilter.AppDirectoryFilter;
import com.sic.bb.hudson.plugins.xcodeplugin.filefilter.ZipFileFilter;
import com.sic.bb.hudson.plugins.xcodeplugin.util.ExtendedFile;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class AppArchiverCallable implements FileCallable<Boolean> {
	private static final long serialVersionUID = 1L;
	
	private final String targetName;
	private final String fileName;
	
	public AppArchiverCallable(String targetName, String fileName) {
		this.targetName = targetName;
		this.fileName = fileName;
	}

	public Boolean invoke(File buildDir, VirtualChannel channel) throws IOException, InterruptedException {
		String[] apps = buildDir.list(new AppDirectoryFilter());
		
		if(apps != null) {
            for(String app: apps) {
            	if(!app.equals(this.targetName + AppDirectoryFilter.FILE_ENDING))
            		continue;
                
	            ExtendedFile appArchiveContents = new ExtendedFile(buildDir,app);
                
                appArchiveContents.zip(new File(buildDir,this.fileName + ZipFileFilter.FILE_ENDING));
                
                return true;
            }
		}
		
		return false;
	}
}
