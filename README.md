# Guice / Resteasy Integration

This is Guice integrated with Resteasy, done the right way (Guice-first).

Guiceeasy is built with/for:
* Java 17+
* Guice 6+
* Resteasy 6+
* The jakarta.* namespace (not javax.*)

## A little history

Long ago, Resteasy provided an extension that integrated with Guice. This extension was problematic because it took a Resteasy-centric view of your application - Resteasy was "the app" which created your Guice injector. This is inside-out; Resteasy is something you should create/mount inside of Guice.  

GWizard is inherently Guice-first, so we found (hacked) a way around this, using the resteasy-guice extension code. 

Then Resteasy abandoned and removed the resteasy-guice extension entirely.

So we rebuilt the integration from scratch. And it turns out this was quite easy - there's only a few dozen lines of code.

## How To Use

Guiceeasy is usable by any Guice project, not just GWizard (though of course, GWizard uses it). There are only two required steps:

1. Add the `GuiceeasyModule` to your injector.
2. Mount the `GuiceeasyFilterDispatcher` or `GuiceeasyHttpServletDispatcher` exactly the way you would normallly mount a Resteasy `FilterDispatcher` or `HttpServletDispatcher`.

Here's an example:

```java
public class MyExampleServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
        install(new GuiceeasyModule());

        // See the documentation for Resteasy HttpServletDispatcher for options
        final Map<String, String> initParams = ImmutableMap.of("resteasy.servlet.mapping.prefix", "/api");
        serve("/api/*").with(GuiceeasyHttpServletDispatcher.class, initParams);
    }
}
```

Note that you probably want to bind a `ContextResolver<ObjectMapper>` in your Guice injector that provides your custom ObjectMapper; simply binding the resolver into the injector registers it with Resteasy.

You can look at [GWizard `RestModule`](https://github.com/gwizard/gwizard/blob/master/gwizard-rest/src/main/java/org/gwizard/rest/RestModule.java) as an example. Of course, you can also just use GWizard!