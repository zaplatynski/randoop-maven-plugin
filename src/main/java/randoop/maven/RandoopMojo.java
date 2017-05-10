package randoop.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Mojo(name="randoop", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RandoopMojo  extends AbstractMojo {

  @Parameter(required = true)
  private String packageName;

  @Parameter(required = true, defaultValue = "${project.build.outputDirectory}")
  private String sourceDirectory;

  @Parameter(required = true,
      defaultValue = "${project.build.directory}/generated-test-sources/java")
  private String targetDirectory;

  @Parameter(required = true, defaultValue = "30")
  private int timeout;

  @Component
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    final List<URL> urls = new LinkedList<>();
    urls.add(loadProjectClasses());
    loadProjectDependencies(urls);
    urls.add(getClass().getProtectionDomain().getCodeSource().getLocation());

    String classPath = urls.stream().map(u -> u.toString()).collect(Collectors.joining(File
        .pathSeparator));
    final List<String> args = new LinkedList<>();
    args.add("java");
    args.add("-ea");
    args.add("-classpath");
    args.add(classPath);
    args.add("randoop.main.Main");
    args.add("gentests");
    args.add("--timelimit="+timeout);
    args.add("--debug-checks=true");
    args.add("--junit-package-name=" + packageName);
    args.add("--junit-output-dir=" + targetDirectory);

    final URLClassLoader classLoader = new URLClassLoader(convert(urls));
    List<Class<?>> allClassesOfPackage = ClassFinder.find(packageName, classLoader);
    for (Class<?> currentClass : allClassesOfPackage) {
      getLog().info("Add class " + currentClass.getName());
      args.add("--testclass=" + currentClass.getName());
    }


    getLog().info("Call: " + args.stream().collect(Collectors.joining(" ")));

    ProcessBuilder processBuilder = new ProcessBuilder(args);
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
    processBuilder.directory(project.getBasedir());
    try {
      Process p = processBuilder.start();
      p.waitFor(timeout+3, TimeUnit.SECONDS);
      if(p.exitValue() != 0){
        throw  new MojoFailureException(this,"Randoop encountered an error!","Failed to generate " +
            "test, exit value is " + p.exitValue());
      }
    } catch (Exception e) {
      throw  new MojoExecutionException(e.getMessage(),e);
    }
  }

  private void loadProjectDependencies(final List<URL> urls) throws MojoExecutionException {
    try {
      for (Artifact artifact : project.getArtifacts()) {
        urls.add(artifact.getFile().toURI().toURL());
      }
    } catch (MalformedURLException e) {
      throw new MojoExecutionException("Could not add artifact!",e);
    }
  }

  private URL loadProjectClasses() throws MojoExecutionException {
    final URL source;
    try {
      source = createUrlFrom(sourceDirectory);
    } catch (MalformedURLException e) {
      throw new MojoExecutionException("Could not create source path!",e);
    }
    return source;
  }

  private static URL[] convert(final Collection<URL> urls) {
    return urls.toArray(new URL[urls.size()]);
  }

  private static URL createUrlFrom(final String path) throws MalformedURLException {
    return new File(path).toURI().toURL();
  }
}
