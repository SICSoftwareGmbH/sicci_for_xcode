package com.sic.bb.hudson.plugins.xcodeplugin.ocunit;

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
