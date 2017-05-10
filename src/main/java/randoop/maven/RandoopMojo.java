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

import randoop.compile.SequenceClassLoader;
import randoop.main.GenTests;
import randoop.main.RandoopTextuiException;

@Mojo(name="randoop", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, threadSafe = false,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RandoopMojo  extends AbstractMojo {

  @Parameter(required = true)
  private String packageName;

  @Parameter(required = true, defaultValue = "${project.build.outputDirectory}")
  private String sourceDirectory;

  @Parameter(required = true,
      defaultValue = "${project.build.directory}/generated-test-sources/java")
  private String targetDirectory;

  @Component
  private MavenProject project;


  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    final List<URL> urls = new LinkedList<>();
    urls.add(loadProjectClasses());
    loadProjectDependencies(urls);

    final URLClassLoader classLoader = new URLClassLoader(convert(urls));

    List<Class<?>> allClassesOfPackage = ClassFinder.find(packageName, classLoader);

    //Build up Randoop args
    final List<String> args = new LinkedList<>();
    args.add("--debug-checks=true");
    args.add("--junit-package-name=" + packageName);
    args.add("--junit-output-dir=" + targetDirectory);
    for (Class<?> currentClass : allClassesOfPackage) {
      getLog().info("Add class " + currentClass.getName());
      args.add("--testclass=" + currentClass.getName());
    }

    ClassLoader l = new SequenceClassLoader(classLoader);

    final GenTests randoop;
    try {
      //Randoop ignores my ClassLoader and uses SystemClassloader so that no classes will be found
      randoop = (GenTests) l.loadClass(GenTests.class.getName()).newInstance();
    } catch (IllegalAccessException|InstantiationException|ClassNotFoundException e) {
      throw new MojoExecutionException("Error occurred while loading Randoop!",e);

    }

    //Generate Tests
    try {
      randoop.handle(convert(args));
    } catch (RandoopTextuiException e) {
      throw new MojoExecutionException("Error occurred while generating tests with Randoop!",e);
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

  private static String[] convert(final List<String> args) {
    return args.toArray(new String[args.size()]);
  }

  private static URL[] convert(final Collection<URL> urls) {
    return urls.toArray(new URL[urls.size()]);
  }

  private static URL createUrlFrom(final String path) throws MalformedURLException {
    return new File(path).toURI().toURL();
  }
}
