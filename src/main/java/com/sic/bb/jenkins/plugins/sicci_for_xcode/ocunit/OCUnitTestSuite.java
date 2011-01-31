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

public class OCUnitTestSuite implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final String testSuiteName;
	private String testSuiteStartTimestamp;
	private String testSuiteEndTimestamp;
	private Vector<OCUnitTestCase> testCases;
	
	public OCUnitTestSuite(String testSuiteName) {
		this.testSuiteName = testSuiteName;
		this.testCases = new Vector<OCUnitTestCase>();
	}
	
	public OCUnitTestSuite(String testSuiteName, String testSuiteStartTimestamp) {
		this.testSuiteName = testSuiteName;
		this.testSuiteStartTimestamp = testSuiteStartTimestamp;
		this.testCases = new Vector<OCUnitTestCase>();
	}
	
	public String getTestSuiteName() {
		return this.testSuiteName;
	}
	
	public void setTestSuiteStartTimestamp(String testSuiteStartTimestamp) {
		this.testSuiteStartTimestamp = testSuiteStartTimestamp;
	}
	
	public String getTestSuiteStartTimestamp() {
		return this.testSuiteStartTimestamp;
	}
	
	public void setTestSuiteEndTimestamp(String testSuiteEndTimestamp) {
		this.testSuiteEndTimestamp = testSuiteEndTimestamp;
	}
	
	public String getTestSuiteEndTimestamp() {
		return this.testSuiteEndTimestamp;
	}
	
	public void addTestCase(OCUnitTestCase testCase) {
		this.testCases.add(testCase);
	}
	
	public Vector<OCUnitTestCase> getTestCases() {
		return this.testCases;
	}
	
	public int getTestCasesCount() {
		return this.testCases.size();
	}
	
	public int getTestCasesFailuresCount() {
		int testCaseFailuresCount = 0;
		
		for(OCUnitTestCase testCase: this.testCases)
			if(testCase.getTestCaseResult() == OCUnitTestCaseResult.FAILED)
				testCaseFailuresCount++;
		
		return testCaseFailuresCount;
	}
	
	public double getTestSuiteDuration() {
		double duration = 0.0;
		
		for(OCUnitTestCase testCase: this.testCases)
			duration += testCase.getTestCaseDuration();
		
		return duration;
	}
}
