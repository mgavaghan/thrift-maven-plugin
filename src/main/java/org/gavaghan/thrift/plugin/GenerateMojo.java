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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * 
 * @author <a href="mailto:mike@gavaghan.org">Mike Gavaghan</a>
 */
@Mojo(name = "generate")
public class GenerateMojo extends AbstractMojo
{
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

		logSettings(maven);

		File sourceIDL = new File(new File(build.getSourceDirectory()).getParent(), "thrift");
		File testIDL = new File(new File(build.getTestSourceDirectory()).getParent(), "thrift");

		try
		{
			searchIDLFolder(build, sourceIDL);
			searchIDLFolder(build, testIDL);
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
	 * @param folder
	 * @throws IOException
	 * @throws MojoExecutionException
	 */
	private void searchIDLFolder(Build build, File folder) throws IOException, MojoExecutionException
	{
		getLog().info("Searching through IDL source folder: " + folder);
		if (!folder.isDirectory() || !folder.canRead()) return;

		for (File file : folder.listFiles())
		{
			if (file.isDirectory())
			{
				searchIDLFolder(build, file);
			}
			else if (file.getName().toLowerCase().endsWith(".thrift"))
			{
				generate(build, file);
			}
		}
	}

	/**
	 * Generate source from an IDL.
	 * 
	 * @param build
	 * @param idl
	 * @throws IOException
	 * @throws MojoExecutionException
	 */
	private void generate(Build build, File idl) throws IOException, MojoExecutionException
	{
		if (!idl.canExecute()) return;

		getLog().info("Generating code from: " + idl);

		// build the command line
		List<String> command = buildCommandLine(build, idl);

		// execute the generator
		if (true)
		{
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.inheritIO();

			Process pr = pb.start();
			int retval;

			try
			{
				retval = pr.waitFor();
				if (retval != 0) throw new MojoExecutionException("Source generation return with error code: " + retval);
			}
			catch (InterruptedException xce)
			{
				throw new MojoExecutionException("Source generation was unexpectedly interrupted");
			}
		}
	}

	private List<String> buildCommandLine(Build build, File idl) throws IOException, MojoExecutionException
	{
		List<String> cmd = new ArrayList<String>();

		// call the executable
		cmd.add(mExecutable);
		
		// recurse through includes
		cmd.add("-r");
		
		// select output folder
		// FIXME distinguish between test and source
		cmd.add("-out");
		
		String outFolder = build.getDirectory() + "/thrift";
		if (!new File(outFolder).mkdirs())
		{
			throw new MojoExecutionException("Unable to create output folder: " + outFolder);
		}
		getLog().info("Output folder: " + outFolder);
		
		cmd.add(outFolder);

		// generate java
		// TODO allow additional options
		cmd.add("-gen");
		cmd.add("java");

		cmd.add(idl.getCanonicalPath());
		return cmd;
	}
}
