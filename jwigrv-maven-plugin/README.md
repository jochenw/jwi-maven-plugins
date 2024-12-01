# The JWI Groovy Maven Plugin

This plugin allows you to run a [Groovy](https://groovy.apache.org) script as part of your Maven build. Using a Groovy
script will typically help you to avoid, what this plugins author did: Implement a Maven plugin, just to do
something, that will be hard, or impossible to do with an existing Maven plugin.

The need for such things is probably much lower than one would assume. Indeed, there are a number of plugins with an
identical, or at least similar purpose. (Examples: The [GMavenPlus](https://github.com/groovy/GMavenPlus) plugin, the
[Groovy Maven Plugin](https://groovy.github.io/gmaven/groovy-maven-plugin/index.html, or the [Maven Antrun plugin](https://maven.apache.org/plugins/maven-antrun-plugin/) However, neither of these appears to be really active, and in use.

The plugins history is simple. I had a [problem](https://issues.apache.org/jira/browse/RAT-379). The problem appeared to
be trivial to solve, if I could run a Groovy script, or the like, within the Maven build. Unfortunately, none of the aforementioned plugins appeared to work. So, I decided to implement my own, which did the job.

## Usage

To use the plugin, have a section like the following in your POM. The purpose of the example is to have the
Groovy script *src/main/build/myGroovyScript.groovy* executed within the *generate-resources* phase.

```XML
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.jochenw.jmp</groupId>
        <artifactId>jwigrv-maven-plugin</artifactId>
        <version>0.1</version> <!-- Or whatever more current version is available -->
        <executions>
          <execution>
            <phase>generate-resources</phase> <!-- You might want a different phase -->
            <goal>run</goal>
            <configuration>
              <scriptFile>src/main/build/myGroovyScript.groovy</scriptFile>
              <scriptProperties>
                <userName>Doe, John</userName>
                <userEmail>john.doe@company.com</userEmail>
              </scriptProperties>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

## Script properties

The above example specifies, that two variables should be available within the script:

- A variable named **userName** with the value *Doe, John*.
- Another variable named **userEmail** with the value *john.doe@company.com*.

In other words, the Groovy interpreter would evaluate the string `"Commit as {userName} with email {userEmail}."`to `"Commit as Doe, John with email john.doe@company.com."`

The variables we have seen so far, are explicitly specified in the Maven POM. In addition to explicitly specified
variables, there are also a few implicitly specified:

| Name    | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| log     | The JWI Groovy Maven plugins [logger](https://maven.apache.org/ref/3.9.9/maven-plugin-api/apidocs/org/apache/maven/plugin/logging/Log.html). In other words, by using this logger, the script can send messages to the Maven console.                                                                                                                                                                                                                                                                                                                                                |
| project | The [Maven project object](https://maven.apache.org/ref/3.9.9/apidocs/org/apache/maven/project/MavenProject.html). Note, that this is a complex, and deeply structured object. For example, the path of the build directory (typically *target*) is available here as **project.build.directory**. Likewise, the path of the Java output directory (typically *target/classes*) can be accessed via **project.build.outputDirectory**.<br/>In particular, the projects Maven coordinates are available here as **project.groupId**, **project.artifactId**, and **project.version**. |

            
