package com.sic.bb.hudson.plugins.xcodeplugin.util;

import hudson.os.PosixAPI;
import hudson.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

public final class ExtendedFile extends File {
	private static final long serialVersionUID = 1L;

	public ExtendedFile(File parent, String child) {
		super(parent, child);
	}

	public void deleteRecursive() {
		if(isFile())
			super.delete();
		else {
			String[] files = list();
			
			if(files == null)
				return;
			else if(files.length == 0)
				super.delete();
			else
				for(String file: files)
					new ExtendedFile(this,file).delete();
		}
	}
	
	public void zip(File zipFile) throws IOException, InterruptedException {
		FileOutputStream zipFileOutputStream = null;
		ZipArchiveOutputStream zipStream = null;
		
		try {
			zipFileOutputStream = new FileOutputStream(zipFile);
			zipStream = new ZipArchiveOutputStream(zipFileOutputStream);
			zipDirectory(this, null, zipStream);
		} finally {
			IOUtils.closeQuietly(zipStream);
			IOUtils.closeQuietly(zipFileOutputStream);
		}
	}
	
    private static void zipDirectory(File file, String path, ZipArchiveOutputStream zipStream) throws IOException, InterruptedException {
    	if(path == null)
    		path = new String();
    	else if(!path.isEmpty())
    		path += File.separatorChar;
    		
    	ZipArchiveEntry zipEntry = new ZipArchiveEntry(file,path + file.getName());
    	zipEntry.setUnixMode(PosixAPI.get().stat(file.getAbsolutePath()).mode());
    	
    	/* TODO: archiving symlinks doesn't work atm
    	zipEntry.setUnixMode(PosixAPI.get().stat(file.getAbsolutePath()).mode());
    	
    	if(Util.isSymlink(file)) {
    		zipEntry = new ZipArchiveEntry(path + file.getName());
    		zipEntry.setUnixMode(PosixAPI.get().stat(file.getAbsolutePath()).mode());
    		
    		AsiExtraField field = new AsiExtraField();
    		field.setLinkedFile(path + file.getName());
    		
    		zipEntry.addExtraField(field);
        	zipStream.putArchiveEntry(zipEntry);
			zipStream.closeArchiveEntry();
        	return;
    	}
    	*/
    	
    	zipStream.putArchiveEntry(zipEntry);
    	
    	if(!file.isDirectory()) {
    		FileInputStream fileInputStream = null;
    		
    		try {
    			fileInputStream = new FileInputStream(file);
    			Util.copyStream(fileInputStream, zipStream);
    		} finally {
    			IOUtils.closeQuietly(fileInputStream);
    			zipStream.closeArchiveEntry();
    		}
		} else {
			zipStream.closeArchiveEntry();
			
	    	String[] entries = file.list();
	    		
	    	if(entries != null)
	    		for(String entry: entries)
	    			zipDirectory(new File(file,entry),path + file.getName(),zipStream);
		}
    }
}
