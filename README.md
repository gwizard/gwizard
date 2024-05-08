# GWizard

GWizard is a modular, opinionated toolkit for building RESTful web services on a Guice backbone. GWizard is
implemented as a set of Guice modules (packaged in separate maven modules) which you can mix and match
to enable features like logging, jaxrs services, hibernate, etc with near-zero boilerplate.

## Help

The best place for help is the gwizard Google Group: https://groups.google.com/forum/#!forum/gwizard

## Code

https://github.com/gwizard/gwizard

## Example Project

The best way to understand GWizard is to fork a simple example project:

https://github.com/gwizard/gwizard-example

## Requirements

GWizard 1.0 requires a minimum of Java 17. It uses:

* Guice 7.x
* Resteasy 6
* Jetty 12
* The `jakarta.*` namespace

The 0.x releases of GWizard use Jetty 9 and the `javax.*` namespace. 

## Overview

GWizard was inspired by Dropwizard, and shares the same goal - to help you build production-ready
RESTful web services with a minimum of boilerplate and fuss. To do that we've made some (sane) decisions for
you, and the first and most important is to use Guice to glue everything together.

The remaining decisions are guided by this philosophy:

* Provide modules, not a container. You write the main() method, you create the Injector.
* Guice creates the web server (or any other kind of server), not the other way 'round.
* Minimize code that exists outside of Guice's cozy embrace. A one-line main() is reasonable:
```java
public static void main(String[] args) throws Exception {
	Guice.createInjector(...some modules...).getInstance(Run.class).start();
}
```
* As much as possible, you should be able to take modules a la carte.
* Components advertise services by binding types.
* Components discover services by injecting types.
* Components are configured by binding/injecting config objects.
* Encourage a single config file which includes configuration across all modules.

## Yeah Yeah, Show Me Some Code

Here's a complete one-file JAX-RS REST service which you can run from the command line:

```java
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.gwizard.rest.RestModule;
import org.gwizard.services.Run;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
		Guice.createInjector(new MyModule(), new RestModule())
			.getInstance(Run.class)
				.start();
	}
}
```

## The Modules

These are the modules currently present in GWizard. See their individual README files for detailed information.
All of these modules are optional, but if you're reading this page, you probably want at least a few of them.

### gwizard-config

The `ConfigModule` loads and binds a configuration object from a YAML file, using Dropwizard's nifty system
property override mechanism.

[README for gwizard-config](gwizard-config/README.md)

### gwizard-logging

The `LoggingModule` sets up Logback and routes all the 'other' logging frameworks into it so you get a single
seamless log. GWizard applications should simply log via SLF4J.

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

### gwizard-services

The `ServicesModule` provides [Guava Services](https://code.google.com/p/guava-libraries/wiki/ServiceExplained),
allowing multiple services to start up and run in parallel. This module is used by gwizard-web and gwizard-metrics
but you can also use it to manage your own services.

If you're already using gwizard-web or gwizard-metrics, you don't need to explicitly include this module.

[README for gwizard-services](gwizard-services/README.md)

### gwizard-metrics

The `MetricsModule` glues in the Metrics library. At the moment, it only adds
a JMX Reporter to report metrics. It also uses metrics-guice to scan Guice-instantiated
classes for @Timed, @Metered and other annotations.

### gwizard-healthchecks

The `HealthCheckModule` glues in the metrics-healthchecks library.

[README for gwizard-healthchecks](gwizard-healthchecks/README.md)

### gwizard-rpc

The `RpcModule` provides a simple and convenient RPC system for pure-Java SOA systems.

[README for gwizard-rpc](gwizard-rpc/README.md)

## Mini-FAQ
We will try to cover some design questions.

### Another framework??
It really isn't. GWizard is just a tiny bit of glue holding together excellent components written by other people.
GWizard is "just a library" with fewer lines of actual code than there are lines of text in the README files.

### What's wrong with Dropwizard? Isn't there a Guice module for DW?
We like DW; as opinionated developers, it's a pleasure to find an opinionated framework
which shares most of our opinions. We just wish it had _one more_ opinion - dependency injection (of
any flavor) would have cut out most of the boilerplate in Dropwizard;
"bag" objects like `Environment` and `Bootstrap` could disappear from the API footprint entirely.
Aside from that:

* Jersey2 does not play nicely with Guice.
* The dropwizard-guice bundle is hobbled by the fact that very little of the framework is instantiated by Guice.
* Dropwizard relies on Jersey for AOP (eg, HibernateBundle's UnitOfWork transaction management). This means you
can't touch your database except through the web container (!); all other tests must mock the DB/DAO.
This is not compatible with our testing philosophy.

GWizard came about because as we progressively Guice-ified a Dropwizard application, we replaced more and more of DW.
This is the logical conclusion of that process. We try to preserve the spirit of Dropwizard, and leverage DW's
code as dependency jars where reasonable.

### What about Dagger instead of Guice?
Dagger2 is great! It would be interesting to build a DagWizard. Unfortunately it doesn't have servlet support
built-in, so there's a big hurdle to jump. 

### How do I deploy a GWizard app?
Make a fat jar and run it like an executable. 
[Dropwizard's example](https://www.dropwizard.io/en/stable/getting-started.html#building-fat-jars) is great.