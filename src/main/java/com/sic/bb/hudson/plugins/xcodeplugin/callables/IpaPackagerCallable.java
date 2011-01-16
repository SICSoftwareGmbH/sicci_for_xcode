package com.sic.bb.hudson.plugins.xcodeplugin.callables;

import java.io.File;
import java.io.IOException;

import com.sic.bb.hudson.plugins.xcodeplugin.ExtendedFile;
import com.sic.bb.hudson.plugins.xcodeplugin.filefilter.AppDirectoryFilter;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class IpaPackagerCallable  implements FileCallable<Boolean> {
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
                
                ipaFileContents.zip(new File(buildDir,this.fileName));
                
                if(!movedAppDir.renameTo(appDir))
                	return false;
                
                ipaFileContents.deleteRecursive();
                
                return true;
            }
		}
		
		return false;
	}
}
