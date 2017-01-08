Beverage Component
==================

Building a 3rd party Apache Camel component.

This is a simple Camel component which demonstrates how to setup the Maven project so the component 
includes JSon metadata which is required by the Camel IDEA plugin to support the component.
  
Camel component developers should pay attention to the `pom.xml` file, and ensure they have setup
the following two entries:  

### camel-apt plugin

Add the following Maven dependency to include the Camel APT compiler plugin which generates the JSon meatadata.

```
    <dependency>
      <groupId>org.apache.camel</groupId>
      <artifactId>apt</artifactId>
      <version>${camel.version}</version>
      <scope>provided</scope>
    </dependency>
```

### camel-package-maven-plugin

This plugin is reponsible for generating additional metadata which are embedded in the JAR file.

```
      <plugin>
        <groupId>org.apache.camel</groupId>
        <artifactId>camel-package-maven-plugin</artifactId>
        <version>${camel.version}</version>
        <executions>
          <execution>
            <goals>
              <goal>generate-components-list</goal>
            </goals>
            <phase>generate-resources</phase>
          </execution>
        </executions>
      </plugin>
```

Having these two in your `pom.xml` file ensures your 3rd party components can be supported by Camel IDEA plugin
and other Camel tooling as well. 

All the components that comes out of the box from Apache Camel does this.


