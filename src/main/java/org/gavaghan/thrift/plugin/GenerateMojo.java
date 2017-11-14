package org.gavaghan.thrift.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Mojo to generate source code from Thrift IDL files.
 * 
 * @author <a href="mailto:mike@gavaghan.org">Mike Gavaghan</a>
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends AbstractMojo
{
	/** Build subdirectory for generated files. */
	static public final String GEN_FOLDER = "thrift";

	/** Build subdirectory for generated test files. */
	static public final String GEN_TEST_FOLDER = "test-thrift";

	/** Path to the thrift compiler executable (if no path, use system path). */
	@Parameter(alias = "executable", required = true)
	private String mExecutable;

	/**
	 * Log runtime settings
	 * 
	 * @param maven
	 */
	private void logSettings(MavenProject maven)
	{
		Build build = maven.getBuild();

		getLog().info("Using thrift compiler: " + mExecutable);
		getLog().debug("Source folder: " + build.getSourceDirectory());
		getLog().debug("Test folder: " + build.getTestSourceDirectory());

		// look at source roots
		List<String> sourceRoots = maven.getCompileSourceRoots();
		for (String root : sourceRoots)
		{
			getLog().debug("Source root: " + root);
		}

		// look at test source roots
		List<String> testSourceRoots = maven.getTestCompileSourceRoots();
		for (String root : testSourceRoots)
		{
			getLog().debug("Test source root: " + root);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.AbstractMojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		Map<?, ?> context = getPluginContext();
		MavenProject maven = (MavenProject) context.get("project");
		Build build = maven.getBuild();

		// log settings from pom
		logSettings(maven);

		// calculate path to IDLs
		File sourceIDL = new File(new File(build.getSourceDirectory()).getParent(), "thrift");
		File sourceGen = new File(build.getDirectory(), GEN_FOLDER);
		File testIDL = new File(new File(build.getTestSourceDirectory()).getParent(), "thrift");
		File testGen = new File(build.getDirectory(), GEN_TEST_FOLDER);

		try
		{
			// add to source roots
			maven.getCompileSourceRoots().add(sourceGen.getCanonicalPath());
			maven.getTestCompileSourceRoots().add(testGen.getCanonicalPath());

			// dive through IDL folders
			searchIDLFolder(build, sourceIDL, sourceGen);
			searchIDLFolder(build, testIDL, testGen);
		}
		catch (IOException exc)
		{
			throw new MojoExecutionException("Failed to generate source from IDL", exc);
		}
	}

	/**
	 * Recursively dive through the IDL folder looking for *.thrift files.
	 * 
	 * @param build
	 *           project Build object
	 * @param folder
	 *           folder to scan for IDL files
	 * @param gen
	 *           target folder for generated source
	 * @throws IOException
	 * @throws MojoExecutionException
	 */
	private void searchIDLFolder(Build build, File folder, File gen) throws IOException, MojoExecutionException
	{
		getLog().info("Searching through IDL source folder: " + folder);
		if (!folder.isDirectory() || !folder.canRead()) return;

		for (File file : folder.listFiles())
		{
			if (file.isDirectory())
			{
				searchIDLFolder(build, file, gen);
			}
			else if (file.getName().toLowerCase().endsWith(".thrift"))
			{
				generate(build, file, gen);
			}
		}
	}

	/**
	 * Generate source from an IDL.
	 * 
	 * @param build
	 *           project Build object
	 * @param idl
	 *           IDL file to generate source for
	 * @param gen
	 *           target folder for generated source
	 * @throws IOException
	 * @throws MojoExecutionException
	 */
	private void generate(Build build, File idl, File gen) throws IOException, MojoExecutionException
	{
		if (!idl.canExecute()) return;

		getLog().info("Generating code from: " + idl);

		// build the command line
		List<String> command = buildCommandLine(build, idl, gen);

		// execute the generator
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.inheritIO();

		getLog().debug("Launching: " + mExecutable);
		
		Process pr = pb.start();
		int retval;

		try
		{
			getLog().debug("Waiting for compiler to return");
			retval = pr.waitFor();
			getLog().debug("Compiler has returned with code: " + retval);
		}
		catch (InterruptedException xce)
		{
			throw new MojoExecutionException("Source generation was unexpectedly interrupted");
		}

		if (retval != 0) throw new MojoExecutionException("Source generation return with error code: " + retval);
	}

	/**
	 * Build the command line to the Thrift compiler
	 * 
	 * @param build
	 *           project Build object
	 * @param idl
	 *           IDL file to generate source for
	 * @param gen
	 *           target folder for generated source
	 * @return
	 * @throws IOException
	 * @throws MojoExecutionException
	 */
	private List<String> buildCommandLine(Build build, File idl, File gen) throws IOException, MojoExecutionException
	{
		List<String> cmd = new ArrayList<String>();

		// call the executable
		cmd.add(mExecutable);

		// recurse through includes
		cmd.add("-r");

		// select output folder
		cmd.add("-out");

		if (!gen.mkdirs())
		{
			if (!gen.exists())  throw new MojoExecutionException("Unable to create output folder: " + gen.getCanonicalPath());
		}
		getLog().info("Output folder: " + gen.getCanonicalPath());

		cmd.add(gen.getCanonicalPath());

		// generate java
		// TODO allow additional options
		cmd.add("-gen");
		cmd.add("java");

		cmd.add(idl.getCanonicalPath());
		return cmd;
	}
}
