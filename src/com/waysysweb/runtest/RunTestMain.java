//------------------------------------------------------------------------------
//      Compilation Unit Header
//------------------------------------------------------------------------------
//
//  Name:           RunTest.java
//  Author:         William A. Shaffer
//  Package:        com.waysysweb.runtest
//
//  Copyright (c) 2011 Waysys, LLC. All Rights Reserved.
//
//
//  Waysys MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
//  THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//  TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
//  PARTICULAR PURPOSE, OR NON-INFRINGEMENT. Waysys SHALL NOT BE LIABLE FOR
//  ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
//  DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
//
//  For further information, contact Waysys LLC at wshaffer@waysysweb.com
//  or 800-622-5315 (USA).
//
//------------------------------------------------------------------------------
//      Maintenance History
//------------------------------------------------------------------------------
//
//  Person    Date          Change
//  ------    -----------   ----------------------------------------------------
//
//  Shaffer   16-Dec-2010   File create
//
//------------------------------------------------------------------------------
//      Package Declaration
//------------------------------------------------------------------------------

package com.waysysweb.runtest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import com.example.unittestcase.TestCaseResult;
import com.waysysweb.RunTestPortType;
import com.waysysweb.RunTest_Service;

//------------------------------------------------------------------------------
//Public Class Declaration
//------------------------------------------------------------------------------

/**
 * This class is the main program for invoking the unit test suite in Guidewire.
 * 
 * This program can be run as follows:
 * 
 * java -jar runtest.jar -testsuite testsuite -reports reports -url http://url
 * -prop prop
 * 
 * where:
 * 
 * testsuite - the directory where the test suite is stored
 * 
 * reports - the file name for the reports
 * 
 * url - the URL for the Guidewire server
 * 
 * prop - the name of the properties file to use
 * 
 * @author W. Shaffer
 * @version 16-Dec-2011
 */
public class RunTestMain {
	// -------------------------------------------------------------------------
	// Fields
	// -------------------------------------------------------------------------

	/** a map with a list of legal properties */
	private Map<String, String> allowedProps;

	/** RunTest properties */
	private Properties runtestProperties;

	private final QName serviceName;

	/** default properties file */
	static final String RUNTEST_PROPERTIES = "runtest.properties";

	/** program version */
	static final String VERSION = "1.00";

	public static TestCaseResult testResult = new TestCaseResult();

	// -------------------------------------------------------------------------
	// Constructor
	// -------------------------------------------------------------------------

	/**
	 * Create an instance of this class
	 */
	/**
	 * 
	 */
	public RunTestMain() {
		//
		// Initialize properties
		//
		runtestProperties = null;
		//
		// Set QName
		//
		serviceName = new QName("http://waysysweb.com", "RunTest");
		//
		// Load list of legal properties
		//
		allowedProps = new HashMap<String, String>(8);
		allowedProps.put("-testsuite", "testsuite");
		allowedProps.put("-reports", "reports");
		allowedProps.put("-url", "url");
		allowedProps.put("-timeout", "timeout");
		allowedProps.put("-prop", "prop");
		allowedProps.put("-username", "username");
		allowedProps.put("-password", "password");
		//
		// Initialize test result
		//
		testResult.setFailed(0);
		testResult.setErrors(0);
		testResult.setSucceeded(0);
		testResult.setErrorNum(0);
	}

	// -------------------------------------------------------------------------
	// Main Program
	// -------------------------------------------------------------------------

	/**
	 * Main program. Return exit value of 0 if tests execute properly. Return 1
	 * if an error occurred in processing.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		int errorNum = 0;
		RunTestMain client = new RunTestMain();
		try {
			errorNum = client.execute(args);
		} catch (Exception e) {
			errorNum = 1;
			testResult = new TestCaseResult();
			testResult.setErrorNum(errorNum);
			testResult.setErrorMessage(e.getMessage());
			System.out.println(e.getMessage());
		}
		System.exit(errorNum);
	}

	/**
	 * Return the test result from an execution.
	 * 
	 * @return the test case result
	 */
	public static TestCaseResult getResults() {
		return testResult;
	}

	// -------------------------------------------------------------------------
	// Execute Web Service
	// -------------------------------------------------------------------------

	/**
	 * Execute the web service
	 * 
	 * @param args
	 *            command line arguments
	 */
	public int execute(String[] args) throws RuntimeException {
		assert args != null;
		System.out.println("Begin RunTest, Version " + VERSION);
		processArgs(args);
		//
		// Get the port
		//
		RunTestPortType port = getPort();
		//
		// Set arguments of the operation
		//
		String testCaseName = getProperty("testsuite");
		String testReportName = getProperty("reports");
		//
		// Check test case name
		//
		if (testCaseName == null) {
			testResult.setErrorMessage("Test suite name is not set");
			testResult.setErrorNum(1);
			testResult.setErrors(1);
		}
		//
		// Check reports file name
		//
		else if (testReportName == null) {
			testResult.setErrorMessage("Report file not set");
			testResult.setErrorNum(1);
			testResult.setErrors(1);
		}
		//
		// Run the operation
		//
		else {
			testResult = port.runTest(testCaseName, testReportName);
			printResults(testResult);
		}
		return testResult.getErrorNum();
	}

	/**
	 * Set up port
	 * 
	 * @return the Run Test Port Type for this service
	 */
	public RunTestPortType getPort() {
		//
		// Get the URL of the server
		//
		URL wsdlLocation = formURL();
		//
		// Get the port
		//
		RunTest_Service service = new RunTest_Service(wsdlLocation, serviceName);
		RunTestPortType port = service.getRunTestSoap11Port();
		//
		// Get user name and password
		//
		String username = getProperty("username");
		if (username == null)
			username = "su";
		String password = getProperty("password");
		if (password == null)
			password = "gw";
		//
		// Set HTTP basic authentication
		//
		BindingProvider bp = (BindingProvider) port;
		Map<String, Object> requestContext = bp.getRequestContext();
		requestContext.put(BindingProvider.USERNAME_PROPERTY, username);
		requestContext.put(BindingProvider.PASSWORD_PROPERTY, password);
		return port;
	}

	/**
	 * Form the URL of the WSDL location
	 * 
	 * @return the URL of the web service WSDL
	 * @throws RuntimeException
	 *             if URL is not provided or if it is malformed
	 */
	public URL formURL() throws RuntimeException {
		String message;
		String urlString;
		URL location = null;
		String server = getProperty("url");
		if (server != null) {
			urlString = server + "/ws/unittestcase/RunTest?WSDL";
			try {
				location = new URL(urlString);
			} catch (MalformedURLException e) {
				message = "Bad server URL - " + server;
				throw new RuntimeException(message);
			}
		} else {
			message = "URL property is not set";
			throw new RuntimeException(message);
		}
		return location;
	}

	/**
	 * Print the results
	 * 
	 * @param result
	 *            the test case results
	 */
	protected void printResults(TestCaseResult result) {
		System.out.println("Tests succeeded: " + result.getSucceeded());
		System.out.println("Tests failed   : " + result.getFailed());
		System.out.println("Test errors    : " + result.getErrors());
		int total = result.getSucceeded() + result.getFailed()
				+ result.getErrors();
		System.out.println("Total tests    : " + total);
		System.out.println("Result is      : " + result.getErrorNum());
		if (result.getErrorNum() != 0) {
			System.out.println("Error: " + result.getErrorMessage());
		}
		return;
	}

	// -------------------------------------------------------------------------
	// Process Command Arguments
	// -------------------------------------------------------------------------

	/**
	 * Process the arguments. Properties in the runtest.properties file are the
	 * default. Users can override the defaults with arguments on the command
	 * line.
	 * 
	 * This function guarantees that, if it completes, the properties file has
	 * been processed and the command line arguments have been processed.
	 * However, there is no guarantee that any particular property has been set
	 * properly.
	 * 
	 * @param args
	 *            an array of strings with arguments
	 * @throws RuntimeException
	 *             if property file cannot be loaded
	 */
	public void processArgs(String[] args) throws RuntimeException {
		//
		// precondition: args != null
		//
		String runtestPropertiesFile = getPropertyFile(args);
		runtestProperties = getProperties(runtestPropertiesFile);
		checkProperties(runtestProperties);
		processCommandArgs(args);
		//
		// postcondition: runtestPropertiesFile != null and
		// properties file has been read and
		// command line arguments have been processed
		//
		return;
	}

	/**
	 * Return the name of the properties file based on scanning the list of
	 * arguments.
	 * 
	 * @param args
	 *            an array of strings with arguments
	 * @return the name of the GFIT properties file
	 */
	public String getPropertyFile(String[] args) {
		String result = RUNTEST_PROPERTIES;
		//
		// Search for "-prop" property
		//
		int count = args.length;
		for (int i = 0; i < count; i++) {
			if (args[i].equals("-prop")) {
				//
				// Is there a following string in the array?
				//
				i++;
				if (i < count)
					result = args[i];
				break;
			}
		}
		//
		// Postcondition: result != null
		//
		return result;
	}

	/**
	 * Process command line arguments. Arguments alternate between a property
	 * name and the property value.
	 * 
	 * @param args
	 *            an array of strings with arguments
	 */
	public void processCommandArgs(String[] args) {
		assert runtestProperties != null;
		int count = args.length - 1;
		String propName;
		String value;
		for (int i = 0; i < count; i += 2) {
			if (allowedProps.containsKey(args[i])) {
				propName = allowedProps.get(args[i]);
				value = args[i + 1];
				value = value.trim();
				runtestProperties.setProperty(propName, value);
			} else
				System.out.println("Unknown property - " + args[i]);
		}
		//
		// Postcondition: all legal online arguments have been processed
		//
		return;
	}

	/**
	 * Return the value of a named property.
	 * 
	 * @param name
	 *            the name of the property
	 * @return a string with the value of the property
	 */
	public String getProperty(String name) {
		assert runtestProperties != null;
		assert name != null;
		return runtestProperties.getProperty(name);
	}

	/**
	 * Set the value of a property.
	 * 
	 * @param name
	 *            the property name
	 * @param value
	 *            the value of the property
	 */
	public void setProperty(String name, String value) {
		runtestProperties.setProperty(name, value);
		return;
	}

	/**
	 * Return a set of properties based on a property file.
	 * 
	 * @param fileName
	 *            the name of the property file
	 * @return a set of properties
	 * @throws ServiceException
	 *             when properties cannot be loaded
	 */
	public Properties getProperties(String fileName) throws RuntimeException {
		//
		// Precondition: fileName != null
		//
		String message = null;
		Properties props = new Properties();
		FileInputStream file = openPropertyFile(fileName);
		if (file != null) {
			try {
				props.load(file);
			} catch (Exception e) {
				message = "Could not load properties file - " + fileName;
				throw new RuntimeException(message);
			}
		} else
			throw new RuntimeException("Unable to get properties");
		//
		// Postcondition: props != null and
		// properties file has been read
		//
		return props;
	}

	/**
	 * Return an instance of file input stream for a properties file. Return
	 * null if the file cannot be opened.
	 * 
	 * @param fileName
	 *            the name of the properties file
	 * @return an instance of file input stream
	 */
	public FileInputStream openPropertyFile(String fileName) {
		FileInputStream file;
		try {
			file = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			System.out.println("Cannot find property file - " + fileName);
			file = null;
		} catch (SecurityException e) {
			System.out.println("Cannot open existing property file - "
					+ fileName);
			file = null;
		}
		return file;
	}

	/**
	 * Check the properties in the property file to insure that they are all
	 * recognized properties.
	 * 
	 * @param properties
	 *            a property set
	 * @throws RuntimeException
	 *             if there is a property that is not restricted
	 */
	public void checkProperties(Properties properties) throws RuntimeException {
		assert properties != null;
		String propName;
		String message = null;
		Enumeration<Object> props = properties.keys();
		while (props.hasMoreElements()) {
			propName = (String) props.nextElement();
			if (!allowedProps.containsValue(propName)) {
				message = "Unrecognized property in property file - "
						+ propName;
				throw new RuntimeException(message);
			}
		}
		return;
	}
}
