package com.github.jochenw.jmp.jwicrl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.io.input.XmlStreamReader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class JwiCrlMojoTest extends AbstractMojoTestCase {
	@BeforeEach protected void setUp() throws Exception {
		final ComponentConfigurator configurator = getContainer().lookup( ComponentConfigurator.class, "basic" );
		setFieldValue(this, "configurator", configurator);

		InputStream is = getClass().getResourceAsStream( "/" + getPluginDescriptorLocation() );

		XmlStreamReader reader = new XmlStreamReader( is );

		final PlexusContainer container = getContainer();
		final Map<Object, Object> contextData = container.getContext().getContextData();
		final Map<String,Object> containerData = (Map<String,Object>) contextData;
		InterpolationFilterReader interpolationFilterReader =
				new InterpolationFilterReader( new BufferedReader( reader ), containerData );

		PluginDescriptor pluginDescriptor = new PluginDescriptorBuilder().build( interpolationFilterReader );

		Artifact artifact =
				lookup( RepositorySystem.class ).createArtifact( pluginDescriptor.getGroupId(),
						pluginDescriptor.getArtifactId(),
						pluginDescriptor.getVersion(), ".jar" );

		artifact.setFile( getPluginArtifactFile() );
		pluginDescriptor.setPluginArtifact( artifact );
		pluginDescriptor.setArtifacts( Arrays.asList( artifact ) );

		for ( ComponentDescriptor<?> desc : pluginDescriptor.getComponents() )
		{
			getContainer().addComponentDescriptor( desc );
		}

		mojoDescriptors = new HashMap<String, MojoDescriptor>();
		for ( MojoDescriptor mojoDescriptor : pluginDescriptor.getMojos() )
		{
			mojoDescriptors.put( mojoDescriptor.getGoal(), mojoDescriptor );
		}
	}
	@AfterEach protected void tearDown() throws Exception { super-setUp(); super.tearDown(); }

	private void setFieldValue(Object pBean, String pFieldName, Object pValue) {
		try {
			final Field field = pBean.getClass().getDeclaredField(pFieldName);
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			field.set(pBean, pValue);
		} catch (Throwable t) {
			throw new UndeclaredThrowableException(t);
		}
	}
	
	@Test
	void testSkip() throws Exception {
		doTest("skip-execution", "", null);
	}

	private File requireTestPom(String pTestId) {
		final Path testDir = Paths.get("src/test/resources/com/github/jochenw/jmp/jwicrl/junit/" + pTestId);
		final File pom = testDir.resolve("pom.xml").toFile();
		assertNotNull(pom);
		assertTrue(pom.toPath().toAbsolutePath().toString(), pom.exists());
		assertTrue(pom.canRead());
		System.setProperty("com.github.jochenw.jmp.jwicrl.BaseDir", testDir.toString());
		return pom;
	}

	private void doTest(String pTestId, String pExpectedOutput, Consumer<Throwable> pErrorValidator) throws Exception, MojoExecutionException, MojoFailureException, UnsupportedEncodingException {
		final File pom = requireTestPom(pTestId);
		final JwiCrlMojo mojo;
		try {
			mojo = (JwiCrlMojo) lookupMojo("request", pom);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw e;
		}
		assertNotNull(mojo);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream savedSystemOut = System.out;
		try {
			System.setOut(new PrintStream(baos));
			mojo.execute();
			if (pErrorValidator != null) {
				Assertions.fail("Expected Exception");
			}
		} catch (MojoExecutionException|MojoFailureException e) {
			if (pErrorValidator != null) {
				pErrorValidator.accept(e);
				return;
			}
			throw e;
		} finally {
			System.setOut(savedSystemOut);
		}
		if (pExpectedOutput != null) {
			final String actualOutput = baos.toString("UTF-8");
			final String expectedOutput = pExpectedOutput.replace("\n", System.lineSeparator());
			Assertions.assertEquals(expectedOutput, actualOutput);
		}
	}
}
