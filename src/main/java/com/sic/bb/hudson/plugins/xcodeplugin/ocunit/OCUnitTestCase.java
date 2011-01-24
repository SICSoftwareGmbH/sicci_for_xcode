package com.sic.bb.hudson.plugins.xcodeplugin.ocunit;

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