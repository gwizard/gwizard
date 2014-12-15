# GWizard

GWizard is a modular, opinionated toolkit for building RESTful web services on a Guice backbone. GWizard is
implemented as a set of Guice modules (packaged in separate maven modules) which you can mix and match
to enable features like logging, jaxrs services, hibernate, etc with near-zero boilerplate.

## Help

The best place for help is the gwizard Google Group: https://groups.google.com/forum/#!forum/gwizard

## Code

https://github.com/stickfigure/gwizard

## Example Project

The best way to understand GWizard is to fork a simple example project:

https://github.com/stickfigure/gwizard-example

## Overview

GWizard was inspired by Dropwizard, and shares the same goal - to help you build production-ready
RESTful web services with a minimum of boilerplate and fuss. To do that we've made some (sane) decisions for
you, and the first and most important is to use Guice to glue everything together.

The remaining decisions are guided by this philosophy:

* Provide modules, not a container. You write the main() method, you create the Injector.
* Guice creates the web server (or any other kind of server), not the other way 'round.
* Minimize code that exists outside of Guice. A one-line main() is reasonable:
```java
public static void main(String[] args) throws Exception {
	Guice.createInjector(...some modules...).getInstance(WebServer.class).startJoin();
}
```
* As much as possible, you should be able to take modules a la carte.
* Components discover components in other modules by binding/injecting types.
* Components are configured by binding/inject config objects.
* Encourage a single config file which includes configuration across all modules.

## Yeah Yeah, Show Me Some Code

Here's a complete JAX-RS REST service:

```java
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.voodoodyne.gwizard.rest.RestModule;
import com.voodoodyne.gwizard.web.WebServer;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

public class Main {
	/** A standard JAX-RS resource class */
	@Path("/hello")
	public static class HelloResource {
		@GET
		public String hello() {
			return "hello, world";
		}
	}

	public static class MyModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(HelloResource.class);
		}
	}

	public static void main(String[] args) throws Exception {
		Guice.createInjector(new MyModule(), new RestModule()).getInstance(WebServer.class).startJoin();
	}
}
```

## The Modules

Note: GWizard is ready to use today, but this project is young and the contracts may change in the future.

These modules are currently present in GWizard. This is just a broad overview; see the README for each
module for details on how to install and use them.

### gwizard-config

The `ConfigModule` loads and binds a configuration object from a YAML file, using Dropwizard's nifty system
property override mechanism. You almost certainly want this.

[README for gwizard-config](gwizard-config/README.md)

### gwizard-logging

The `LoggingModule` sets up logback and routes all the 'other' logging frameworks into it - jakarta commons logging,
log4j, and java.util.logging (using the LevelChangePropagator). GWizard applications should simply log via SLF4J.

[README for gwizard-logging](gwizard-logging/README.md)

### gwizard-web

The `WebModule` provides a Jetty webserver configured to allow you to use Guice ServletModules in your application.
If you are building JAX-RS REST services, you don't need to install this module directly; use the `RestModule`
(gwizard-rest) which installs this module for you.

[README for gwizard-web](gwizard-web/README.md)

### gwizard-rest

The `RestModule` bakes in RESTEasy and turns any JAX-RS-annotated classes which have been registered
in your modules into REST endpoints. Installing this module automatically installs the `WebModule`.

[README for gwizard-rest](gwizard-rest/README.md)

### gwizard-hibernate

The `HibernateModule` provides a little bit of glue around guice-persist to give you Hibernate (as a JPA
provider) without a lot of boilerplate.

[README for gwizard-hibernate](gwizard-hibernate/README.md)

## Mini-FAQ
We will try to cover some design questions.

### Another framework?
It really isn't. GWizard is just a tiny bit of glue holding together

### What's wrong with Dropwizard? Isn't there a Guice module for DW?
We like DW; as opinionated developers, it's a pleasure to find an opinionated framework
which shares most of our opinions. We just wish it had _one more_ opinion - dependency injection (of
any flavor) would have cut out a lot of of the boilerplate in Dropwizard.
"Bag" objects like Environment and Bootstrap could disappear from the API footprint entirely.
Aside from that:

* Jersey2 does not play nicely with Guice (see below).
* The dropwizard-guice bundle is hobbled by the fact that very little of the framework is instantiated by Guice.
* Dropwizard relies on Jersey for AOP (eg, HibernateBundle's UnitOfWork transaction management). This means you
can't touch your database except through the web container (!); all other tests must mock the DB/DAO.
This is not compatible with our testing philosophy.

GWizard came about because as we progressively Guice-ified a Dropwizard application, we replaced more and more of DW.
This is the logical conclusion of that process. We try to preserve the spirit of Dropwizard, and leverage DW's
code as dependency jars where reasonable.

However, Dropwizard is a *vastly* more mature framework, with *many* more features, and *lovely* documentation.
There's really no comparison!

### Why did you pick RESTEasy over Jersey?
We didn't, originally, which is how we discovered what a trainwreck Jersey2 has become.

Jersey1 worked great with Guice. For Jersey2, the team wrote their own DI framework from scratch
(HK2) and littered it with global state, config files in META-INF/service,
and other JavaEE-isms that make my skin crawl. In the year and a half that Jersey2 has been
released, nobody has managed to make it play 100% nicely with Guice. The
[most valiant attempt](https://github.com/Squarespace/jersey2-guice) uses reflection to punch
values into private final fields in HK2's static globals, and it still
[isn't seamless](https://github.com/Squarespace/jersey2-guice/issues/17).

If I sound bitter writing this, it's because I wasted a day and a half struggling to make it to work.
Which it mostly does; you can dig that code out of a branch if you want it. By comparison, RESTEasy
comes pre-baked with Guice integration that worked almost immediately.

I'm sorry I doubted you, Team JBoss.

The truth is it doesn't really matter which JAX-RS framework you use. You write your resource
classes the same either way.

### What about Dagger instead of Guice?
Dagger2 looks great! We'll consider migrating (or creating DWizard) just as soon as it supports AOP.
Aspects are just too useful for managing transaction state, identity, etc. AOP doesn't appear to
be on the Dagger2 roadmap yet, and in the mean time, Guice works great.
