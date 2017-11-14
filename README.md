# thrift-maven-plugin v1.0.0
Thrift plugin for generating Java code from a Thrift IDL

**Author:** Mike Gavaghan - **Email:** mike@gavaghan.org

## Description ##
This plugin defines a very simple goal: `generate`

This goal looks for IDL files in `/src/main/thrift` and `/src/test/thrift`.  The IDLs are compiled into java code and saved to `/target/thrift` and `/target/test-thrift` during the `generate-sources` phase.  These folder are then added to the java source folder list to be compiled during the `compile` phase. 

## Usage ##
Add this section to your `pom.xml`:

    <build>
       <plugins>
          <plugin>
             <groupId>org.gavaghan</groupId>
             <artifactId>thrift-maven-plugin</artifactId>
             <version>1.0.0</version>
             <configuration>
                <!--
                   Specify your Thrift compiler executable.  If the executable is not
                   on your PATH, you'll need to provide a fully qualified path.
                -->
                <executable>thrift-0.10.0.exe</executable>
             </configuration>
             <executions>
                <execution>
                   <goals>
                      <goal>generate</goal>
                   </goals>
                </execution>
             </executions>
          </plugin>
       </plugins>
    </build>
    
Execute this command to generate source code from your IDLs:

    mvn clean generate-sources

