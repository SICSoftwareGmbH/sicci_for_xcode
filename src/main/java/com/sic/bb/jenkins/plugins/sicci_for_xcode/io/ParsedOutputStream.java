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
import java.util.Vector;

import com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit.OCUnitTestSuite;

public class ParsedOutputStream extends LineTransformationOutputStream {
	private final OCUnitOutputParser ocUnitParser;
	private final OutputStream logger;
	
	public ParsedOutputStream(OutputStream logger) {
		this.ocUnitParser = new OCUnitOutputParser();
		this.logger = logger;
	}
	
	public Vector<OCUnitTestSuite> getParsedTests() {
		return this.ocUnitParser.getParsedTests();
	}
	
	protected void eol(byte[] bytes, int len) throws IOException {
	    String line = new String(bytes, 0, len);
	    this.ocUnitParser.parse(line);
	    this.logger.write(line.getBytes());
	}

}
