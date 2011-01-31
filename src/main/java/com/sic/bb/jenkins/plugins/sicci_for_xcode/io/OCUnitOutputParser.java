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

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit.OCUnitTestCase;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit.OCUnitTestCaseError;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit.OCUnitTestCaseResult;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit.OCUnitTestSuite;

public class OCUnitOutputParser {
	private static final Pattern testStarted =
		Pattern.compile("^\\s*Test\\s+Suite\\s+'\\s*(\\S.+(?:\\.octest).*\\S)\\s*'\\s+started\\s+at\\s+(\\S.+\\S)\\s*$");
	
	private static final Pattern testFinished =
		Pattern.compile("^\\s*Test\\s+Suite\\s+'\\s*(\\S.+(?:\\.octest).*\\S)\\s*'\\s+finished\\s+at\\s+(\\S.+\\S)\\s*$");
	
	private static final Pattern testSuiteStarted =
		Pattern.compile("^\\s*Test\\s+Suite\\s+'\\s*(\\S.+\\S)\\s*'\\s+started\\s+at\\s+(\\S.+\\S)\\s*$");
	
	private static final Pattern testSuiteFinished =
		Pattern.compile("^\\s*Test\\s+Suite\\s+'\\s*(\\S.+\\S)\\s*'\\s+finished\\s+at\\s+(\\S.+\\S)\\s*\\.\\s*$");
	
	private static final Pattern testCaseStarted =
		Pattern.compile("^\\s*Test\\s+Case\\s+'-\\[\\s*(\\S.+\\S)\\s*\\]'\\s+started\\s*\\.\\s*$");
	
	private static final Pattern testCasePassed =
		Pattern.compile("^\\s*Test\\s+Case\\s+'-\\[\\s*(\\S.+\\S)\\s*\\]'\\s+passed\\s+\\(\\s*([0-9.]+)\\s+seconds\\s*\\)\\s*\\.\\s*$");

	private static final Pattern testCaseFailed =
		Pattern.compile("^\\s*Test\\s+Case\\s+'-\\[\\s*(\\S.+\\S)\\s*\\]'\\s+failed\\s+\\(\\s*([0-9.]+)\\s+seconds\\s*\\)\\s*\\.\\s*$");
	
	private static final Pattern testCaseError =
		Pattern.compile("^\\s*(\\S.+\\S)\\s*:\\s*(\\d+)\\s*:\\s+error:\\s+-\\[\\s*(\\S.+\\S)\\s*\\]\\s+:\\s+(\\S.+\\S)\\s*$");
	
	private Vector<OCUnitTestSuite> testSuites;
	
	
	public OCUnitOutputParser() {
		this.testSuites = new Vector<OCUnitTestSuite>();
	}
	
	public Vector<OCUnitTestSuite> getParsedTests() {
		return this.testSuites;
	}
	
	public void parse(String line) {
		if(matchTestStarted(line))
			return;
		
		if(matchTestSuiteStarted(line))
			return;
		
		if(matchTestCaseStarted(line))
			return;
		
		if(matchTestCaseError(line))
			return;

		if(matchTestCasePassed(line))
			return;
		
		if(matchTestCaseFailed(line))
			return;
		
		if(matchTestFinished(line))
			return;
		
		if(matchTestSuiteFinished(line))
			return;
	}
	
	private boolean matchTestStarted(String line) {
		Matcher matcher = testStarted.matcher(line);
		
		if(!matcher.matches())
			return false;
		
		return true;
	}
	
	private boolean matchTestFinished(String line) {
		Matcher matcher = testFinished.matcher(line);
		
		if(!matcher.matches())
			return false;
		
		return true;
	}
	
	private boolean matchTestSuiteStarted(String line) {
		Matcher matcher = testSuiteStarted.matcher(line);
		
		if(!matcher.matches())
			return false;
		
		this.testSuites.add(new OCUnitTestSuite(matcher.group(1),matcher.group(2)));
		
		return true;
	}
	
	private boolean matchTestSuiteFinished(String line) {
		Matcher matcher = testSuiteFinished.matcher(line);
		
		if(!matcher.matches() || !this.testSuites.lastElement().getTestSuiteName().equals(matcher.group(1)))
			return false;
		
		this.testSuites.lastElement().setTestSuiteEndTimestamp(matcher.group(2));
		
		return true;
	}
	
	private boolean matchTestCaseStarted(String line) {
		Matcher matcher = testCaseStarted.matcher(line);
		
		if(!matcher.matches())
			return false;
		
		OCUnitTestSuite lastTestSuite = this.testSuites.lastElement();
		
		if(!matcher.group(1).contains(lastTestSuite.getTestSuiteName()))
			return false;
	
		String testCaseName = StringUtils.strip(StringUtils.substringAfter(matcher.group(1),lastTestSuite.getTestSuiteName()));
		
		this.testSuites.lastElement().addTestCase(new OCUnitTestCase(testCaseName));
		
		return true;
	}
	
	private boolean matchTestCasePassed(String line) {
		Matcher matcher = testCasePassed.matcher(line);
		
		if(!matcher.matches())
			return false;
		
		OCUnitTestCase lastTestCase = this.testSuites.lastElement().getTestCases().lastElement();
		
		if(!matcher.group(1).contains(lastTestCase.getTestCaseName()))
			return false;
		
		double duration = -1.0;
		
		try {
			duration = Double.parseDouble(matcher.group(2));
		} catch(NumberFormatException e) {
		}
		
		lastTestCase.setTestCaseResult(OCUnitTestCaseResult.PASSED);
		lastTestCase.setTestCaseDuration(duration);
		
		return true;
	}
	
	private boolean matchTestCaseFailed(String line) {
		Matcher matcher = testCaseFailed.matcher(line);
		
		if(!matcher.matches())
			return false;
		
		OCUnitTestCase lastTestCase = this.testSuites.lastElement().getTestCases().lastElement();

		if(!matcher.group(1).contains(lastTestCase.getTestCaseName()))
			return false;
		
		double duration = 0.000;
		
		try {
			duration = Double.parseDouble(matcher.group(2));
		} catch(NumberFormatException e) {
		}
		
		lastTestCase.setTestCaseResult(OCUnitTestCaseResult.FAILED);
		lastTestCase.setTestCaseDuration(duration);
		
		return true;
	}
	
	private boolean matchTestCaseError(String line) {
		Matcher matcher = testCaseError.matcher(line);
		
		if(!matcher.matches())
			return false;
		
		OCUnitTestCase lastTestCase = this.testSuites.lastElement().getTestCases().lastElement();

		if(!matcher.group(3).contains(lastTestCase.getTestCaseName()))
			return false;
		
		int faultyLine = -1;
		
		try {
			faultyLine = Integer.parseInt(matcher.group(2));
		} catch(NumberFormatException e) {
		}
		
		OCUnitTestCaseError testCaseError = new OCUnitTestCaseError(matcher.group(1), faultyLine, matcher.group(4));
		
		lastTestCase.addTestCaseError(testCaseError);
		
		return true;
	}
}