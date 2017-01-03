Camel IDEA Plugin
=================

Plugin for Intellij IDEA to provide a set of small Camel related capabilities to IDEA editor.

The plugin includes:

- Smart completion for Camel endpoints in Java, Groovy, Scala, XML, properties or yaml files (`ctrl + space`)
- Smart completion for Camel property placeholders (cursor after `{{`)
- Intention to add new Camel endpoint in Java editor (`alt + enter` in empty string)
- Quick documentation for Camel endpoints and external link to Camel component opening in web browser (`ctrl + j` and `shift-F1`)

When the plugin becomes more complete and stable then the intention is to donate the source code
to Apache Software Foundation to be included out of the box at Apache Camel.
 
However currently the code is located at github to allow faster and wider collaboration in the community.

![Early Screenshot](https://github.com/davsclaus/camel-idea-plugin/blob/master/img/early2.png)

### Build status
[![Build Status](https://travis-ci.org/davsclaus/camel-idea-plugin.svg?branch=master)](https://travis-ci.org/davsclaus/camel-idea-plugin)

### How to install

The plugin `Apache Camel IDEA plugin` is available from Jetbrains Plugin Repository at: https://plugins.jetbrains.com/idea/plugin/9371-apache-camel-idea-plugin

You should be able to install the plugin from within IDEA plugin manager.
Open the `Preference` menu and select `Plugins`. Click the `Browse repositories...` if `Apache Camel IDEA plugin` is not
available in the plugin list. From there you should be able to browse all the plugins from the Jetbrains remote plugin repository.


### How to try

Currently the plugin is editing Java endpoints in Java source code.

You can open any Camel example which uses Java code, such as `camel-example-spring-boot` an
select the `MySpringBootRouter` route class and position the cursor on any of the Camel endpoints
after the '?' mark and press `ctrl + space`. 

Currently its only endpoint options in the URI query section which can be edited. Its planned to add
support for editing the options in the URI context-path section as well.


### Building from source

You can build the plugin from source code, which requires to setup IDEA for plugin development.

You can follow the guide from Jetbrains here: http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html

To be able to browse the IDEA SDK source code you can clone the IDEA community source code, as described
in the guide above. I did this so I can peak inside their source code, because you need this to find out
how to hook into various IDEA APIs which is massive and takes longer time to figure out to use. Sadly
Jetbrains are not very good at documenting their APIs with neither javadoc, or documentation to their own plugins.
However with some trial and run you can find out bit by bit.

#### Importing project as maven project

Importing the project into IntelliJ as plug-in project requires a few steps before it possible.

First you need to install the "Intellij plugin development with Maven" in your IDEA

 > - Open Preferences -> Plugins
 > - Browse for plug-ins and search "Intellij plugin development with Maven"
 > - Install the plug-in by right click and press "Download and Install"
 > - Restart the IDE
 
Second you need to install the IntelliJ libraries into your local maven repository. The first parameter
is the version of the IDEA you have installed locally, second is the locations of the IDEA.

 > - Execute the script file "./install-intellij-libs.sh 2016.3.2 /Applications/IntelliJ\ IDEA\ CE.app/Contents"
  
Next step you need to update the pom.xml file with the right Intellij version number

 > - Open the pom.xml file
 > - Modify the property idea.version with the version number you have installed.
 > - Run "mvn install" or "mvn install -P ultimate" for running the Ultimate version of IDEA

Last step you need to import the project as maven project.

> Important : if you are using the Ultimate version you need to enable the maven profile "uiltimate" when importing the
project. Otherwise running the test will not work from IDEA

> - Open your IDEA
> - Create a new project from existing source
> - Select the "camel-idea-plugin" location
> - Import project from external module and select Maven
> - Press next until you hit the page "Please select the project SDK"
> - Press the "+" and add new "IntelliJ Platform Plugin project"
> - Press next and finish
> - Open the "Module Settings" and select the tab "Plug-in Deployment" 
> - Make sure the path to the "META-INF/plugin.xml" point to the "src/main/resources/" directory

### Running and debugging the plugin from source

After completing all steps and if everything is setup correctly, then you can launch the plugin by 
add a plugin run/debug configuration. To do that, go to Run → Edit Configurations…​ and add Plugin configuration.
                                                                  
You can also launch the plugin in debug mode where you can put breakpoints in the source code.
This is very handy to debug the code and find issues. However for code changes you need to stop and
start the plugin again.

### Running the unit test

Running the IntelliJ unit test from maven with the community version

> mvn verify

Running the IntelliJ unit test from maven with the Ultimate version

> mvn verify -P ultimate

Running the test from IDEA community requires to add following settings to the run configuration VM options.

> -ea
  -Xbootclasspath/p:../out/classes/production/boot
  -XX:+HeapDumpOnOutOfMemoryError
  -Xmx256m
  -XX:MaxPermSize=320m
  -Didea.system.path=target/test-system
  -Didea.home.path=target/
  -Didea.config.path=target/test-config
  -Didea.test.group=ALL_EXCLUDE_DEFINED
  

> For the Ultimate version no VM options is necessary   

### Contributing / Hacking on the code

We love contributions. And anyone is welcome to join and hack on the code. For code changes you
can submit github PRs (pull requests) which anyone can review and get merged into the code base.

For people who hack more on the code, can be granted commit rights.

You should be willing to provide any code changes under the ASF license and that the code later
will be donated to Apache Software Foundation to be included out of the box at Apache Camel.
If you are not willing to accept this, then we are sorry, but then any code contributions cannot be accepted.

### IDEA SDK and FAQ

Jetbrains provides a FAQ for the IDEA SDK which is massive and takes time to learn.

- http://www.jetbrains.org/intellij/sdk/docs/faq.html

### TODOs

The plugin is far from finished/ready, and therefore there is work to be done. 

The issue tracker has a list of tickets with items to implement. This can be a good place
to look for _stuff_ you can help with.

Also we love feedback and you are welcome to log tickets about issues, ideas, etc.


### Screenshots

We will post various screen shots of the plugin in the
[img directory](https://github.com/davsclaus/camel-idea-plugin/tree/master/img)
which you can browse.
