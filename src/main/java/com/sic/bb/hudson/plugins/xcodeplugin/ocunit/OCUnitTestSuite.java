package com.sic.bb.hudson.plugins.xcodeplugin.ocunit;

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
}
