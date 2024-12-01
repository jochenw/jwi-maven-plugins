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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

@Mojo(name="run")
public class JwiGrvMojo extends AbstractMojo {
	@Parameter(property="jmpg.skip", defaultValue="false") 
	private boolean skip;

	@Parameter(property="jmpg.script", required=true)
	private String scriptFile;

	@Parameter
	private Map<String,String> scriptProperties;

	@Parameter
	private MavenProject project;

	protected Path getScriptFile() throws MojoFailureException {
		if (scriptFile == null) {
			throw new MojoFailureException("The parameter 'scriptFile' is null.");
		}
		if (scriptFile.trim().length() == 0) {
			throw new MojoFailureException("The parameter 'scriptFile' is empty.");
		}
		final File baseDir;
		if (project == null) {
			// For testing: The JwiGrvMojoTest uses this to inject a base directory.
			final String baseDirProperty = System.getProperty("com.github.jochenw.jmp.jwigrv.BaseDir");
			if (baseDirProperty != null) {
				baseDir = Paths.get(baseDirProperty).toFile();
			} else {
				baseDir = null;
			}
		} else {
			baseDir = project.getBasedir();
		}
		final Path scriptFilePath;
		if (baseDir == null) {
			scriptFilePath = Paths.get(scriptFile);
		} else {
			scriptFilePath = baseDir.toPath().resolve(scriptFile);
		}
		if (Files.isRegularFile(scriptFilePath)) {
			if (Files.isReadable(scriptFilePath)) {
				return scriptFilePath;
			} else {
				throw new MojoFailureException("Invalid value for parameter 'scriptFile': "
						+ "Expected readable file, got '" + scriptFile + "', resolved to "
						+ scriptFilePath.toAbsolutePath());
			}
		} else {
			throw new MojoFailureException("Invalid value for parameter 'scriptFile': "
					+ "Expected existing file, got '" + scriptFile + "', resolved to "
					+ scriptFilePath.toAbsolutePath());
		}
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skipping execution, because 'skip' parameter is true.");
		}
		final Path scriptFile = getScriptFile();
		final GroovyShell gsh = new GroovyShell();
		final Script script;
		try {
			script = gsh.parse(scriptFile.toFile());
		} catch (IOException ioe) {
			throw new MojoExecutionException("Failed to compile script file: " + scriptFile + ": " + ioe.getMessage(), ioe);
		}
		final Binding binding = new Binding();
		binding.setProperty("log", getLog());
		if (scriptProperties != null) {
			scriptProperties.forEach((k,v) -> binding.setProperty(k, v));
		}
		if (project != null) {
			binding.setProperty("project", project);
		}
		script.setBinding(binding);
		script.run();
	}
}
