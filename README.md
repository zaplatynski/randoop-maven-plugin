# randoop-maven-plugin
A plugin for using Randoop in Maven. See https://randoop.github.io for more information.

## Todo
1. Use maven dependency for Randoop as soon as available
1. Check if if need all or which configuration parameters from Randoop should be available via 
Maven.
1. Should the ErrorTest be excluded by default since it is red until code change?
1. Fix redirect console from Randoop process into Maven for logging purposes

## How to use

A brief description of needed steps.

### 1. Step
Install into local Maven repository:
```
mvn clean install
```
To override Randoop version 3.1.5 if needed run instead:
```
mvn clean install -Drevision=new.version
```
A new version of Randoop will be download from the GitHub releases.

### 2. Step
Integrate into another (local) Maven project by adding to the plugins section the following 
plugin:
 
```xml
<build>
    ...
    </plugins>
        ...
        <plugin>
            <groupId>randoop</groupId>
            <artifactId>randoop-maven-plugin</artifactId>
            <version>3.1.5</version>
            <configuration>
                <packageName>my.base.package</packageName>
            </configuration>
            <executions>
                <execution>
                    <id>generate-tests</id>
                    <goals>
                        <goal>gentests</goal>
                    </goals>
                    <phase>generate-test-sources</phase>
                </execution>
            </executions>
        </plugin>
        ...
    </plugins>
    ...
</build>
```
It will collect all class from the (base) package name and run Randoop.

### 3. Step
Run `mvn clean test` in the Maven project from step 2 to let the test be generated and executed.