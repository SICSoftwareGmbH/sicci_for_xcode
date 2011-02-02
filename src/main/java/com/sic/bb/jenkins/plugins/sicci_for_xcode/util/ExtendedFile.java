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

package com.sic.bb.jenkins.plugins.sicci_for_xcode.util;

import hudson.Util;
import hudson.os.PosixAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

public class ExtendedFile extends File {
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
