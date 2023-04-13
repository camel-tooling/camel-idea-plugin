Apache Camel IDEA Plugin
========================

[![Chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://camel.zulipchat.com/)
[![Twitter](https://img.shields.io/twitter/follow/ApacheCamel.svg?label=Follow&style=social)](https://twitter.com/ApacheCamel)

Plugin for Intellij IDEA to provide a set of Apache Camel related capabilities to the code editor.

The plugin includes:

- Code completion for Camel endpoints in Java, XML, properties or YAML files (`ctrl + space`)
- Code completion for Camel message headers (available `setHeader` and `header`) in Java, XML and YAML files (`ctrl + space`) with corresponding Quick documentation (`ctrl + j`)
- Code completion for Camel property placeholders (cursor after `{{`)
- Code completion for Camel options' key and value in properties and YAML files (`ctrl + space`) with corresponding Quick documentation (`ctrl + j`)
- Real time validation for Camel endpoints in Java, XML, YAML (underline errors in red)
- Real time validation for Camel simple language in Java, XML, YAML (underline errors in red)
- Endpoint options filtered to only include applicable options when used as consumer vs producer only mode
- Quick navigation to other Camel routes routing to this route by clicking the Camel icon in the gutter
- Intention to add new Camel endpoint (`alt + enter` in empty string)
- Quick documentation for Camel endpoints and external link to Camel component opening in web browser (`ctrl + j` and `shift-F1`)
- Show endpoint information in tooltip when hovering mouse over from/to etc in Java route builders
- Supports 3rd party Camel components (if they have been properly built with Camel JSon schema metadata)
- Attempts to use same version as camel-core dependency from the loaded project (may require download over internet)
- Inspection (analyze code) to validate Camel endpoints in Java, XML, YAML
- Camel icon in gutter can be customized by choosing one of the three provided icons
- Supports loading camel-catalog from third party Maven repositories define in the project Maven pom.xml file
- Defines the Camel Runtime (set manually or detected automatically) to adapt the completion of options' key and value in properties files accordingly (by default automatic detection is enabled)
- Auto setup of the Camel Debugger for different Camel runtimes (Standalone/Main, SpringBoot, Quarkus). In case of Camel Quarkus, it is only possible using the Camel Quarkus runner.
- Evaluate Camel expressions and set the body, Headers or Exchange properties from the Debugger Window thanks to custom actions
- Set the body, Headers or Exchange properties from the context menu of the Debugger Window using the Camel simple language

When the plugin becomes more complete and stable then the intention is to donate the source code
to Apache Software Foundation to be included out of the box at Apache Camel.
 
However currently the code is located at github to allow faster and wider collaboration in the community.

![Screenshot](https://github.com/camel-idea-plugin/camel-idea-plugin/blob/main/img/24-option-smart-completion.gif)


### How to install

The plugin `Apache Camel` is available from Jetbrains Plugin Repository at: https://plugins.jetbrains.com/idea/plugin/9371-apache-camel-idea-plugin

You should be able to install the plugin from within IDEA plugin manager.
Open the `Preference` menu and select `Plugins`. Click the `Markeplace...` and type `Apache Camel` in the search box to find the plugin,
and then you can install it.

### How to try

The plugin is editing Java endpoints in Java source code.

You can open any Camel example which uses Java code, such as the [Camel Spring Boot Example](https://github.com/apache/camel-spring-boot-examples/tree/main/spring-boot)
and select the `MySpringBootRouter` route class and position the cursor on any of the Camel endpoints
after the '?' mark and press `ctrl + space`. 

Currently, its only endpoint options in the URI query section which can be edited. It's planned to add
support for editing the options in the URI context-path section as well.


### Plugin Preferences

The plugin comes with a preference where you can configure global settings for the plugin such as turning on or off the real time validation in the editor, or whether to show the Camel icon in the gutter, etc.
If you want to change the default preferences open the `Preferences...` menu, select `Languages & Frameworks` and `Apache Camel`. Here are screenshots of it:
![Screenshot](https://github.com/camel-tooling/camel-idea-plugin/blob/main/img/26-plugin-preferences-1.png)
![Screenshot](https://github.com/camel-tooling/camel-idea-plugin/blob/main/img/27-plugin-preferences-2.png)


### <a name="buildingfromsource"></a> Building from source

You can build the plugin from source code, which requires to setup IDEA for plugin development.

You can follow the guide from Jetbrains here: http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html

To be able to browse the IDEA SDK source code you can clone the IDEA community source code, as described
in the guide above. I did this so I can peak inside their source code, because you need this to find out
how to hook into various IDEA APIs which is massive and takes longer time to figure out to use. Sadly
Jetbrains are not very good at documenting their APIs with neither javadoc, or documentation to their own plugins.
However with some trial and run you can find out bit by bit.


#### Importing project as gradle project

Importing the project into IntelliJ as plug-in only require you choose ìmport from external model` and select gradle


> Important : if you are using the Ultimate version you can set a gradle property in your gradle.properties `intellij_type=IU`

> - Open your IDEA
> - Create a new project from existing source
> - Select the "camel-idea-plugin" location
> - Import project from external module and select Gradle
> - Press next until you hit the page "Please select the project SDK"
> - Press the "+" and add new "IntelliJ Platform Plugin project"
> - Press next and finish
> - Open the "Module Settings" and select the tab "Plug-in Deployment" 
> - Make sure the path to the "META-INF/plugin.xml" point to the "src/main/resources/" directory

#### <a name="runningwithpreviousversion"></a>Running the plug-in with a previous versions of IDEA

The plugin is tested with `IDEA 2022.1` or newer, but if you want to try with a older version you can follow this guide

> - Follow the guide [build from source](#buildingfromsource)
> - Change the attribute `<idea-version since-build="221"/>` in `camel-idea-plugin/src/main/resources/META-INF/plugin.xml` to match the version. please see [document](http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html) for build number description 
> - Build the source with `./gradlew build` 
> - The new plugin zip file `camel-idea-plugin-<version>-SNAPSHOT.jar` is located in `camel-idea-plugin/build/libs`
> - Install the plugin from disk in IDEA preferences


### Running and debugging the plugin from source

After completing all steps and if everything is setup correctly, then you can launch the plugin by running the
gradle task `runIde` and for building and running test run the gradle task `build`

![gradle task](https://github.com/camel-tooling/camel-idea-plugin/blob/main/img/setup/camel-idea-plugin-gradle.png)

You can also launch the plugin in debug mode where you can put breakpoints in the source code.
This is very handy to debug the code and find issues. However for code changes you need to stop and
start the plugin again.


### Running the unit test

Running the IntelliJ unit test from gradle with the community version

> gradle test

### Contributing / Hacking on the code

We love contributions. Anyone is welcome to join and hack on the code. For code changes you
can submit GitHub PRs (pull requests) which anyone can review and get merged into the code base.

For people who hack more on the code, can be granted commit rights.

You should be willing to provide any code changes under the ASF license and that the code later
will be donated to Apache Software Foundation to be included out of the box at Apache Camel.
If you are not willing to accept this, then we are sorry, but then any code contributions cannot be accepted.


### IDEA SDK and FAQ

Jetbrains provides a FAQ for the IDEA SDK which is massive and takes time to learn.

- http://www.jetbrains.org/intellij/sdk/docs/faq.html

They also provide a forum for API Plugin Development.
- https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development

For Gitter Channel
- https://gitter.im/IntelliJ-Plugin-Developers

### Camel IDEA Plugin FAQ

We created a FAQ page to help other developers with common errors when working with the plugin sources.

- https://github.com/camel-idea-plugin/camel-idea-plugin/wiki/Frequently-Asked-Questions

### Screenshots

We will post various screenshots of the plugin in the
[img directory](https://github.com/camel-idea-plugin/camel-idea-plugin/tree/main/img)
which you can browse.
