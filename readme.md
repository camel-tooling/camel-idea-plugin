Apache Camel IDEA Plugin
========================

[![GitHub tag](https://img.shields.io/github/tag/camel-idea-plugin/camel-idea-plugin.svg?style=plastic)]()
[![Build Status](https://api.travis-ci.org/camel-tooling/camel-idea-plugin.svg?branch=master)](https://www.travis-ci.org/camel-tooling/camel-idea-plugin)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)]()
[![Gitter](https://img.shields.io/gitter/room/camel-tooling/Lobby.js.svg)](https://gitter.im/camel-tooling/Lobby)


Plugin for Intellij IDEA to provide a set of Apache Camel related capabilities to the code editor.

The plugin includes:

- Code completion for Camel endpoints in Java, XML, properties or yaml files (`ctrl + space`)
- Code completion for Camel property placeholders (cursor after `{{`)
- Real time validation for Camel endpoints in Java, XML (underline errors in red)
- Real time validation for Camel simple language in Java, XML (underline errors in red)
- Endpoint options filtered to only include applicable options when used as consumer vs producer only mode
- Quick navigation to other Camel routes routing to this route by clicking the Camel icon in the gutter
- Intention to add new Camel endpoint (`alt + enter` in empty string)
- Quick documentation for Camel endpoints and external link to Camel component opening in web browser (`ctrl + j` and `shift-F1`)
- Show endpoint information in tooltip when hovering mouse over from/to etc in Java route builders
- Supports 3rd party Camel components (if they have been properly built with Camel JSon schema metadata)
- Attempts to use same version as camel-core dependency from the loaded project (requires Camel 2.16.1 or newer and may require download over internet)
- Inspection (analyze code) to validate Camel endpoints in Java, XML
- Camel icon in gutter can be customized by choosing one of the three provided icons
- Supports loading camel-catalog from third party Maven repositories define in the project Maven pom.xml file 
- Supports Maven, Gradle, and SBT based projects

When the plugin becomes more complete and stable then the intention is to donate the source code
to Apache Software Foundation to be included out of the box at Apache Camel.
 
However currently the code is located at github to allow faster and wider collaboration in the community.

![Screenshot](https://github.com/camel-idea-plugin/camel-idea-plugin/blob/master/img/24-option-smart-completion.gif)


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


### Plugin Preferences

The plugin comes with a preference where you can configure global settings for the plugin such as turning on or off the real time validation in the editor, or whether to show the Camel icon in the gutter, etc.
If you want to change the default preferences open the `Preferences...` menu, select `Languages & Frameworks` and `Apache Camel`. Here are screenshots of it:
![Screenshot](https://github.com/camel-tooling/camel-idea-plugin/blob/master/img/26-plugin-preferences-1.png)
![Screenshot](https://github.com/camel-tooling/camel-idea-plugin/blob/master/img/27-plugin-preferences-2.png)

### IDEA Compatibility

The Camel IDEA plugin currently requires IDEA 2016.2 or newer. If you are using a older version of IDEA and still want to try the plugin, follow the guide [here](#runningwithpreviousversion)

The current plugin uses `since-build 162`

IDEA provides more information about their SDK versions here: http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html


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

The plugin is tested with `IDEA 2016.2` or newer, but if you want to try with a older version you can follow this guide

> - Follow the guide [build from source](#buildingfromsource)
> - Change the attribute `<idea-version since-build="162.0"/>` in `camel-idea-plugin/src/main/resources/META-INF/plugin.xml` to match the version. please see [document](http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html) for build number description 
> - Build the source with `mvn install` 
> - The new plugin zip file `camel-idea-plugin-<version>-SNAPSHOT.jar` is located in `camel-idea-plugin/target`
> - Install the plugin from disk in IDEA preferences


### Running and debugging the plugin from source

After completing all steps and if everything is setup correctly, then you can launch the plugin by running the
gradle task `runIde` and for building and running test run the gradle task `build`

![gradle task](https://github.com/camel-tooling/camel-idea-plugin/blob/master/img/camel-idea-plugin-gradle.png)

You can also launch the plugin in debug mode where you can put breakpoints in the source code.
This is very handy to debug the code and find issues. However for code changes you need to stop and
start the plugin again.


### Running the unit test

Running the IntelliJ unit test from maven with the community version

> gradle test

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

They also provide a forum for API Plugin Development.
- https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development

For Gitter Channel
- https://gitter.im/IntelliJ-Plugin-Developers

### Camel IDEA Plugin FAQ

We created a FAQ page to help other developers with common errors when working with the plugin sources.

- https://github.com/camel-idea-plugin/camel-idea-plugin/wiki/Frequently-Asked-Questions

### TODOs

The issue tracker has a list of tickets with items to implement. This can be a good place
to look for _stuff_ you can help with. We have labeled the beginner tickets with `beginner` and `help wanted`.

Also we love feedback and you are welcome to log tickets about issues, ideas, etc.


### Screenshots

We will post various screen shots of the plugin in the
[img directory](https://github.com/camel-idea-plugin/camel-idea-plugin/tree/master/img)
which you can browse.
