Camel IDEA Plugin
=================

Plugin for Intellij IDEA to provide a set of small Camel related capabilities to IDEA editor.

The plugin includes:

- Smart completion for Camel endpoints in Java editor (in progress)
- Smart completion for Camel endpoints in XML editor (planned)
- Inspection for validating Camel endpoints (planned)

When the plugin becomes more complete and stable then the intention is to donate the source code
to Apache Software Foundation to be included out of the box at Apache Camel.
 
However currently the code is located at github to allow faster and wider collaboration in the community.

![Early Screenshot](https://github.com/davsclaus/camel-idea-plugin/blob/master/img/early2.png)

### How to try

We plan to publish the plugin in the IDEA plugin manager, so you can install the plugin from IDEA.

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

I have put screen shots of how I have setup IDEA for plugin development. There is a couple of fields
which you must configure with an absolute file location to a directory, and why its important to
setup this correctly. Those fields has been marked with red rectangle in the screenshots.

The screen shots are in the [img/setup directory](https://github.com/davsclaus/camel-idea-plugin/tree/master/img/setup).

### Running and debugging the plugin from source

If everything is setup correctly, then you can launch the plugin, by running "Apache Camel IDEA plugin"
from the run menu in IDEA. Then a 2nd instance of IDEA is launched where you can open a Camel project.

You can also launch the plugin in debug mode where you can put breakpoints in the source code.
This is very handy to debug the code and find issues. However for code changes you need to stop and
start the plugin again.


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
