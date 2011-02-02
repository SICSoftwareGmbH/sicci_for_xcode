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

package com.sic.bb.jenkins.plugins.sicci_for_xcode.callables;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

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

import com.sic.bb.jenkins.plugins.sicci_for_xcode.filefilter.XmlFileFilter;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit.OCUnitTestCase;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit.OCUnitTestCaseError;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit.OCUnitTestCaseResult;
import com.sic.bb.jenkins.plugins.sicci_for_xcode.ocunit.OCUnitTestSuite;

public class OCUnitToJUnitWriterCallable implements FileCallable<Boolean>{
	private static final long serialVersionUID = 1L;
	
	public static final String filePreamble = "TEST-";
	
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
	        	
	        	Element testSuiteElement = document.createElement("testsuite");
	        	testSuiteElement.setAttribute("errors","0"); // TODO
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
	        	}
	        	
	        	testSuiteElement.setAttribute("time", String.valueOf(testSuite.getTestSuiteDuration()));
	        	
	        	document.appendChild(testSuiteElement);
	        	
	        	// StreamResult got a problem with spaces in file path
	        	FileOutputStream fileOutputStream = 
	        		new FileOutputStream(new File(dir,filePreamble + testSuite.getTestSuiteName() + XmlFileFilter.FILE_ENDING));
	        		        	
	            transformer.transform(new DOMSource(document), new StreamResult(fileOutputStream));
	        }
			
			return true;
		} catch(Exception e) {
		}
		
		return false;
	}
}
