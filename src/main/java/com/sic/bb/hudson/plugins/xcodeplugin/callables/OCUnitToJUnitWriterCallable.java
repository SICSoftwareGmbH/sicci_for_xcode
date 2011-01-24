package com.sic.bb.hudson.plugins.xcodeplugin.callables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.sic.bb.hudson.plugins.xcodeplugin.ocunit.OCUnitTestCase;
import com.sic.bb.hudson.plugins.xcodeplugin.ocunit.OCUnitTestCaseError;
import com.sic.bb.hudson.plugins.xcodeplugin.ocunit.OCUnitTestCaseResult;
import com.sic.bb.hudson.plugins.xcodeplugin.ocunit.OCUnitTestSuite;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class OCUnitToJUnitWriterCallable implements FileCallable<Boolean>{
	private static final long serialVersionUID = 1L;
	
	private final Vector<OCUnitTestSuite> testSuites;
	
	public OCUnitToJUnitWriterCallable(Vector<OCUnitTestSuite> testSuites) {
		this.testSuites = testSuites;
	}

	public Boolean invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
		try {
	        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        	Transformer transformer = TransformerFactory.newInstance().newTransformer();
	        
	        for(OCUnitTestSuite testSuite: this.testSuites) {
	        	Document document = documentBuilder.newDocument();
	        	
	        	double executionTime = 0.0;
	        	
	        	Element testSuiteElement = document.createElement("testsuite");
	        	testSuiteElement.setAttribute("errors","0");
	        	testSuiteElement.setAttribute("failures", String.valueOf(testSuite.getTestCasesFailuresCount()));
	        	testSuiteElement.setAttribute("hostname", "Hudson"); // TODO
	        	testSuiteElement.setAttribute("name", testSuite.getTestSuiteName());
	        	testSuiteElement.setAttribute("tests", String.valueOf(testSuite.getTestCasesCount()));
	        	testSuiteElement.setAttribute("timestamp", testSuite.getTestSuiteStartTimestamp());
	        	
	        	for(OCUnitTestCase testCase: testSuite.getTestCases()) {
	        		Element testCaseElement = document.createElement("testcase");
	        		testCaseElement.setAttribute("classname", testSuite.getTestSuiteName());
	        		testCaseElement.setAttribute("name", testCase.getTestCaseName());
	        		testCaseElement.setAttribute("time", String.valueOf(testCase.getTestCaseDuration()));
	        		
	        		if(testCase.getTestCaseResult() == OCUnitTestCaseResult.FAILED) {
	        			for(OCUnitTestCaseError testCaseError: testCase.getTestCaseErrors()) {
	        				Element testCaseErrorElement = document.createElement("failure");
	        				testCaseErrorElement.setAttribute("message", testCaseError.getTestCaseErrorMessage());
	        				testCaseErrorElement.setAttribute("type", "Failure");
	        				
	        				Text testCaseErrorText = document.createTextNode(testCaseError.getTestCaseFaultyFile() 
	        						+ ":" + testCaseError.getTestCaseFaultyLine());
	        				
	        				testCaseErrorElement.appendChild(testCaseErrorText);
	        				testCaseElement.appendChild(testCaseErrorElement);
	        			}
	        		}
	        		
	        		testSuiteElement.appendChild(testCaseElement);
	        		executionTime += testCase.getTestCaseDuration();
	        	}
	        	
	        	testSuiteElement.setAttribute("time", String.valueOf(executionTime));
	        	
	        	document.appendChild(testSuiteElement);
	        	
	        	// StreamResult got a problem with spaces in file path
	        	FileOutputStream fileOutputStream = new FileOutputStream(new File(dir,"TEST-" + testSuite.getTestSuiteName() + ".xml"));
	        		        	
	            transformer.transform(new DOMSource(document), new StreamResult(fileOutputStream));
	        }
			
			return true;
		} catch(Exception e) {
		}
		
		return false;
	}
}
