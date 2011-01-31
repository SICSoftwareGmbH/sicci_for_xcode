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

package com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit;

import java.io.Serializable;

public class OCUnitTestCaseError implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String testCaseFaultyFile;
	private int testCaseFaultyLine;
	private String testCaseErrorMessage;
	
	public OCUnitTestCaseError(String faultyFile, int faultyLine, String errorMessage) {
		this.testCaseFaultyFile = faultyFile;
		this.testCaseFaultyLine = faultyLine;
		this.testCaseErrorMessage = errorMessage;
	}
	
	public String getTestCaseFaultyFile() {
		return this.testCaseFaultyFile;
	}
	
	public int getTestCaseFaultyLine() {
		return this.testCaseFaultyLine;
	}
	
	public String getTestCaseErrorMessage() {
		return this.testCaseErrorMessage;
	}
}
