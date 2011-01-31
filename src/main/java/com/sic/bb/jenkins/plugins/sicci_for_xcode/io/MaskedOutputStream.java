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

package com.sic.bb.jenkins.plugins.sicci_for_xcode.io;

import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.sic.bb.jenkins.plugins.sicci_for_xcode.util.PluginUtils;

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
