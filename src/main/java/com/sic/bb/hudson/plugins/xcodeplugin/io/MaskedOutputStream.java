package com.sic.bb.hudson.plugins.xcodeplugin.io;

import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.sic.bb.hudson.plugins.xcodeplugin.util.PluginUtils;

public class MaskedOutputStream extends LineTransformationOutputStream {
	private static final String MASK = "******";
	
	private final OutputStream logger;
	private Pattern toMaskPattern;
	
	public MaskedOutputStream(OutputStream logger, String toMask) {
		this.logger = logger;
		
		List<String> toSuppressList = new ArrayList<String>();
		toSuppressList.add(toMask);
		this.toMaskPattern = PluginUtils.createPattern(toSuppressList);
	}
	
	public MaskedOutputStream(OutputStream logger, List<String> toSuppressList) {
		this.logger = logger;
		
		this.toMaskPattern = PluginUtils.createPattern(toSuppressList);
	}

	@Override
	protected void eol(byte[] bytes, int len) throws IOException {
	    String line = new String(bytes, 0, len);
	    line = this.toMaskPattern.matcher(line).replaceAll(MASK);
	    this.logger.write(line.getBytes());
	}
}
