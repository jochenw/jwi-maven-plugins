/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.jochenw.jmp.jwigrv;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwiGrvMojoTest extends AbstractMojoTestCase {
	@BeforeEach protected void setUp() throws Exception { super.setUp(); }
	@AfterEach protected void tearDown() throws Exception { super.tearDown(); }

	@Test
	void testSimpleRun() throws Exception {
		doTest("simple-run", "Hello, world!\n");
	}

	private void doTest(String pTestId, String pExpectedOutput) throws Exception, MojoExecutionException, MojoFailureException, UnsupportedEncodingException {
		final File pom = requireTestPom(pTestId);
		final JwiGrvMojo mojo;
		try {
			mojo = (JwiGrvMojo) lookupMojo("run", pom);
		} catch (RuntimeException rte) {
			rte.printStackTrace(System.err);
			throw rte;
		}
		assertNotNull(mojo);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream savedSystemOut = System.out;
		try {
			System.setOut(new PrintStream(baos));
			mojo.execute();
		} finally {
			System.setOut(savedSystemOut);
		}
		final String actualOutput = baos.toString("UTF-8");
		final String expectedOutput = pExpectedOutput.replace("\n", System.lineSeparator());
		Assertions.assertEquals(expectedOutput, actualOutput);
	}

	@Test
	void testSkippedRun() throws Exception {
		doTest("skipped-run", "[INFO] Skipping execution, because 'skip' parameter is true.\n");
	}

	@Test
	void testVariables() throws Exception {
		final String expectedOutput = "Hello, world!\n"
				+ "userName=Doe, John\n"
				+ "userEmail=john.doe@company.com\n";
		doTest("variables", expectedOutput);
	}
	private File requireTestPom(String pTestId) {
		final Path testDir = Paths.get("src/test/resources/com/github/jochenw/jmp/jwigrv/junit/" + pTestId);
		final File pom = testDir.resolve("pom.xml").toFile();
		assertNotNull(pom);
		assertTrue(pom.toPath().toAbsolutePath().toString(), pom.exists());
		assertTrue(pom.canRead());
		System.setProperty("com.github.jochenw.jmp.jwigrv.BaseDir", testDir.toString());
		return pom;
	}

}
