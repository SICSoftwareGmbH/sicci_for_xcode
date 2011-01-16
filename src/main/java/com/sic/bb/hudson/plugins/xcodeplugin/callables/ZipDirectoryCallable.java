package com.sic.bb.hudson.plugins.xcodeplugin.callables;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class ZipDirectoryCallable implements FileCallable<Void> {
	private static final long serialVersionUID = 1L;
	
	private final OutputStream zipOutputStream;
	
	public ZipDirectoryCallable(OutputStream zipOutputStream) {
		this.zipOutputStream = zipOutputStream;
	}

	public Void invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
        ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(this.zipOutputStream);
        zipDirectory(file,null,zipStream);
        zipStream.close();
		
		return null;
	}
	
    private static void zipDirectory(File file, String path, ZipArchiveOutputStream zipStream) throws IOException, InterruptedException {
    	if(path == null)
    		path = new String();
    	else if(!path.isEmpty())
    		path += File.separatorChar;
    	
    	FilePath filePath = new FilePath(file);
    		
    	ZipArchiveEntry zipEntry = new ZipArchiveEntry(file,path + file.getName());
    	
    	if(filePath.mode() != -1)
    		zipEntry.setUnixMode(filePath.mode());
    	
    	zipStream.putArchiveEntry(zipEntry);
    	
    	if(!file.isDirectory()) {
    		filePath.copyTo(zipStream);
    		zipStream.closeArchiveEntry();
		} else {
			zipStream.closeArchiveEntry();
			
	    	String[] entries = file.list();
	    		
	    	if(entries != null)
	    		for(String entry: entries)
	    			zipDirectory(new File(file,entry),path + file.getName(),zipStream);
		}
    }
}
