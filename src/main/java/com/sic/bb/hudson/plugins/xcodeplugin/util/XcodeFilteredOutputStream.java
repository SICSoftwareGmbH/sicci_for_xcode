package com.sic.bb.hudson.plugins.xcodeplugin.util;

import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class XcodeFilteredOutputStream extends LineTransformationOutputStream {
	private static final String MASK = "******";
	private static final String[] REGEXSPECIALCHARS = new String[] {"(",")","{","}","[","]","^","$","*",".","?","|"};
	
	private final OutputStream logger;
	private final Pattern toSuppressPattern;
	
	public XcodeFilteredOutputStream(OutputStream logger, List<String> toSuppress) {
		this.logger = logger;
		
	    StringBuilder regex = new StringBuilder();
	    
	    if(toSuppress != null) {
	      regex.append('(');
	      
	      for(String item: toSuppress) {
	        if(StringUtils.isBlank(item))
	          continue;

	        for(String regexChar: REGEXSPECIALCHARS)
	        	item = item.replace(regexChar, "\\" + regexChar);
	        
	        regex.append(item);
	        regex.append('|');
	      }
	      
	      regex.deleteCharAt(regex.length() - 1);
	      regex.append(')');
	    }
	    
	    this.toSuppressPattern = Pattern.compile(regex.toString());
	}
	
	@Override
	protected void eol(byte[] bytes, int len) throws IOException {
	    String line = new String(bytes, 0, len);
	    line = this.toSuppressPattern.matcher(line).replaceAll(MASK);
	    this.logger.write(line.getBytes());
	}
}
