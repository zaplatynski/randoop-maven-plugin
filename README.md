# randoop-maven-plugin
A plugin for using Randoop in Maven.

## How to use

A brief description of needed steps.

### 1. Step
Install into local Maven repository:
```
mvn clean install
```
To override Randoop version 3.1.5 run:
```
mvn clean install -Drevision=new.version
```

### 2. Step
Integrate in another local Maven project by adding to the plugins section the following 
plugin:
 
```xml
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
                <goal>randoop</goal>
            </goals>
            <phase>generate-test-sources</phase>
        </execution>
    </executions>
</plugin>
```
It will collect all class from the (base) package name and try to run Randoop. Which will not 
work due to class loading right now.