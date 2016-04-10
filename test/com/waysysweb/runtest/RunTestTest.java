//------------------------------------------------------------------------------
//      Compilation Unit Header
//------------------------------------------------------------------------------
//
//  Copyright (c) 2011, 2012 Waysys LLC All Rights Reserved.
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
//  Shaffer   19-Dec-2011   File create
//  Shaffer   07-Dec-2012   Moved from default package
//
//------------------------------------------------------------------------------
//      Package Declaration
//------------------------------------------------------------------------------

package com.waysysweb.runtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.security.Permission;
import java.util.Properties;

import org.junit.Test;

import com.example.unittestcase.TestCaseResult;

//------------------------------------------------------------------------------
//      Public Class Declaration
//------------------------------------------------------------------------------

/**
 * This class tests the RunTest program.
 * 
 * Note: To run this test, set the working directory in the Run Configuration to
 * C:\workspace\RunTest\src
 * 
 * @author William A. Shaffer
 * @version 1.00 07-Dec-2012
 * 
 */

public class RunTestTest {
	// ------------------------------------------------------------------------------
	// Fields
	// ------------------------------------------------------------------------------

	protected RunTestMain runner;

	protected String[] defaultArgs;

	/**
	 * Set this again in the child classes. The 0th element is "-prop" and the
	 * the 1st element is the default properties file.
	 */
	private static final String[] DEFAULT_ARGS = { "-prop", "runtest.properties" };

	// ------------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------------

	/**
	 * Create an instance of this class.
	 */
	public RunTestTest() {
		super();
		defaultArgs = DEFAULT_ARGS;
		return;
	}

	/**
	 * This set up is from
	 * http://stackoverflow.com/questions/309396/java-how-to-
	 * test-methods-that-call-system-exit
	 * 
	 * @author Derkeiler.com
	 * 
	 */
	protected static class ExitException extends SecurityException {
		static final long serialVersionUID = 999;

		/** contains the status returned from System.exit() */
		public final int status;

		/**
		 * construct a new exception
		 * 
		 * @param status
		 *            the integer status from System.exit()
		 */
		public ExitException(int status) {
			super("There is no escape!");
			this.status = status;
		}
	}

	/**
	 * A simple, substitute security manager that throws an exception when the
	 * System.exit function is called.
	 * 
	 * @author Derkeiler.com
	 * 
	 */
	public static class NoExitSecurityManager extends SecurityManager {
		/**
		 * Do nothing to check permissions
		 */
		@Override
		public void checkPermission(Permission perm) {
			// allow anything.
		}

		/**
		 * Do nothing to check permissions
		 */
		@Override
		public void checkPermission(Permission perm, Object context) {
			// allow anything.
		}

		/**
		 * Throw an Exit Exception with the indicated status
		 * 
		 * @param status
		 *            an integer with the status from errorNum. Either 0 or 1.
		 * @throws ExitException
		 *             all the time
		 */
		@Override
		public void checkExit(int status) {
			super.checkExit(status);
			throw new ExitException(status);
		}
	}

	// ------------------------------------------------------------------------------
	// Test Support Functions
	// ------------------------------------------------------------------------------

	/**
	 * Perform initial setup
	 * 
	 */
	@org.junit.Before
	public void setUp() {
		System.setSecurityManager(new NoExitSecurityManager());
		runner = new RunTestMain();
		return;
	}

	/**
	 * Perform final teardown
	 * 
	 */
	@org.junit.After
	public void tearDown() {
		System.setSecurityManager(null); // or save and restore original
		runner = null;
		return;
	}
	
	// ------------------------------------------------------------------------------
	// Tests Support
	// ------------------------------------------------------------------------------

	/**
	 * Perform a successful test
	 * 
	 * @param args
	 *            the arguments used for the test
	 */
	public void execGoodTest(String[] args) {
		try {
			RunTestMain.main(args);
		} catch (ExitException e) {
			//
			// Normal termination expected
			//
			assertTrue(e.status == 0);
			TestCaseResult r = RunTestMain.getResults();
			assertEquals(4, r.getSucceeded());
			assertEquals(2, r.getFailed());
			assertEquals(0, r.getErrors());
			assertEquals(0, r.getErrorNum());
		}
		return;
	}

	/**
	 * Perform a failure test
	 * 
	 * @param args
	 *            the arguments used for the test
	 */
	public void execFailTest(String[] args) {
		try {
			RunTestMain.main(args);
		} catch (ExitException e) {
			//
			// Error termination expected
			//
			assertTrue(e.status == 1);
			TestCaseResult r = RunTestMain.getResults();
			assertEquals(r.getErrorNum() , 1);
			assertTrue(r.getErrorMessage().length() > 0);
		}
		return;
	}

	// ------------------------------------------------------------------------------
	// Product Independent Tests
	// ------------------------------------------------------------------------------

	/**
	 * Test access to properties file
	 */
	@Test
	public void testAccessToProperties() {
		FileInputStream file = null;
		try {
			file = runner.openPropertyFile("runtest.properties");
			file.close();
		} catch (Exception e) {
			assertTrue("Unable to access property file", false);
		}
		return;
	}

	/**
	 * Test handling file not found
	 */
	@Test
	public void testUnknownFile() {
		try {
			FileInputStream file = runner.openPropertyFile("XX");
			assertTrue("Incorrect handling of unkown property file",
					file == null);
		} catch (Exception e) {
			assertTrue(e.getMessage(), false);
		}
	}

	/**
	 * Test loading of properties
	 */
	@Test
	public void testLoadingProperties() {
		Properties props = null;
		try {
			props = runner.getProperties("runtest.properties");
		} catch (RuntimeException e) {
			fail("Exception thrown opening runtest properties: " + e.getMessage());
		}
		if (props != null)
			assertTrue("Props did not load", !props.isEmpty());
	}

	/**
	 * Test the loading of RunTest properties
	 */
	@Test
	public void testRunTestProperties() {
		String[] args = {};
		try {
			runner.processArgs(args);
		} catch (RuntimeException e) {
			// ignore exception
		}
		String url = runner.getProperty("url");
		assertEquals(url, "http://localhost:8080/cc");
		return;
	}

	/**
	 * TestCase003 - Test the processing of command line arguments
	 */
	@Test
	public void testCommand() {
		String[] args = { 
				"-testsuite", "aaa", "-reports", "yyy", "-url", "vvv",
        };
		try {
			runner.processArgs(args);
		} catch (RuntimeException e) {
			// ignore exception
		}
		assertEquals(runner.getProperty("testsuite"), args[1]);
		assertEquals(runner.getProperty("reports"), args[3]);
		assertEquals(runner.getProperty("url"), args[5]);
		return;
	}

	/**
	 * Report properties on the command line that are not allowed
	 */
	@Test
	public void testPropertyNotAllowed() {
		String[] args = { "-reports", "billy", "-url", "jack",
				"-testsuite", "aaa", "-zzz", "yyy" };
		try {
			runner.processArgs(args);
		} catch (RuntimeException e) {
			// ignore exception
		}
		assertEquals(runner.getProperty("reports"), args[1]);
		assertEquals(runner.getProperty("url"), args[3]);
		assertEquals(runner.getProperty("testsuite"), args[5]);
		return;
	}

	/**
	 * Test zero command arguments
	 */
	@Test
	public void testZeroArgument() {
		String[] args = {};
		try {
			runner.processArgs(args);
		} catch (RuntimeException e) {
			// ignore exception
		}
		assertEquals(runner.getProperty("testsuite"), "unittestcase.SampleTestSuite");
		return;
	}

	/**
	 * Test incorrect number of arguments
	 */
	@Test
	public void testOddNumberArguments() {
		String[] args = { "-testsuite" };
		try {
			runner.processArgs(args);
		} catch (RuntimeException e) {
			// ignore exception
		}
		assertEquals(runner.getProperty("testsuite"), "unittestcase.SampleTestSuite");
		return;
	}

	/**
	 * Test of report any properties in the property set that are
	 * unrecognized properties
	 */
	@Test
	public void testUnrecognizedProp() {
		String[] args = { "-prop", "no.properties" };
		execFailTest(args);
		return;
	}

	/**
	 * Test testsuite property not set
	 */
	@Test
	public void testTestsuiteNotSet() {
		String[] args = { "-prop", "notestsuite.properties" };
		execFailTest(args);
	}

	/**
	 * Test reports property not set
	 */
	@Test
	public void testReportsNotSet() {
		String[] args = { "-prop", "noreports.properties" };
		execFailTest(args);
	}

	/**
	 * Test url property not set
	 */
	@Test
	public void testUrlNotSet() {
		String[] args = { "-prop", "nourl.properties" };
		execFailTest(args);
	}

	/**
	 * Test of no property file
	 */
	@Test
	public void testNoPropFile() {
		String[] args = { "-prop", "nosuchfile.properties" };
		execFailTest(args);
	}

	/**
	 * Incorrect Host in URL
	 */
	@Test
	public void testIncorrectHost() {
		String[] args = { "-url", "http://xx" };
		execFailTest(args);
	}

	// ------------------------------------------------------------------------------
	// Product Specific Tests
	// ------------------------------------------------------------------------------

	/**
	 * Run the test suite. Set the default args in the constructor of the
	 * children classes. No need to override in child classes.
	 */
	@Test
	public void testExecute() {
		execGoodTest(defaultArgs);
		return;
	}

	/**
	 * Test of space before a property value
	 */
	@Test
	public void testSpaceBeforeValue() {
		String[] args = { "", "", "-testsuite", " unittestcase.SampleTestSuite" };
		args[0] = defaultArgs[0];
		args[1] = defaultArgs[1];
		execGoodTest(args);
	}	
	
	/**
	 * Execute test with invalid test suite.
	 */
	@Test
	public void testIncorrectTestSuite() {
		String[] args = { "", "", "-testsuite", "/xx" };
		args[0] = defaultArgs[0];
		args[1] = defaultArgs[1];
		execFailTest(args);
	}

	/**
	 * Execute test with invalid reports.
	 */
	@Test
	public void testIncorrectReports() {
		String[] args = { "", "", "-reports", "/||" };
		args[0] = defaultArgs[0];
		args[1] = defaultArgs[1];
		execFailTest(args);
	}
	
	/**
	 * Execute test with policy center
	 */
	@Test
	public void testPCExecute() {
		String[] args = {"-prop", "pcruntest.properties"};
		execGoodTest(args);                   
		return;
	}

	/**
	 * Test of user name without soapadmin permission
	 */
	@Test
	public void testPermission() {
		String[] args = {"-username", "anazabal"};
		execFailTest(args);
		return;
	}
	
}

