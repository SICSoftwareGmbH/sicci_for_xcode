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
import java.util.Vector;

public class OCUnitTestCase implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final String testCaseName;
	private double testCaseDuration;
	private OCUnitTestCaseResult testCaseResult;
	private Vector<OCUnitTestCaseError> testCaseErrors;
	
	public OCUnitTestCase(String testCaseName) {
		this.testCaseName = testCaseName;
		this.testCaseErrors = new Vector<OCUnitTestCaseError>();
	}
	
	public void setTestCaseResult(OCUnitTestCaseResult testCaseResult) {
		this.testCaseResult = testCaseResult;
	}
	
	public OCUnitTestCaseResult getTestCaseResult() {
		return this.testCaseResult;
	}
	
	public String getTestCaseName() {
		return this.testCaseName;
	}
	
	public void setTestCaseDuration(double testCaseDuration) {
		this.testCaseDuration = testCaseDuration;
	}
	
	public double getTestCaseDuration() {
		return this.testCaseDuration;
	}
	
	public void addTestCaseError(OCUnitTestCaseError testCaseError) {		
		this.testCaseErrors.add(testCaseError);
	}
	
	public int getTestCaseErrorsCount() {
		return this.testCaseErrors.size();
	}
	
	public Vector<OCUnitTestCaseError> getTestCaseErrors() {
		return this.testCaseErrors;
	}
}