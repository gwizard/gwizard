# GWizard RPC

`RpcModule` is a simple and convenient Java RPC system, based on HTTP and [Trivet](https://github.com/stickfigure/trivet).
Using Trivet instead of REST/JSON has some advantages:

 * You code your application as a vanilla Java interface and implementation.
 * You can transmit polymorphic object graphs with arbitrary internal references and cycles.
 * Exception stacktraces are propagated whole - even through multiple layers of RPC.
 * Mocking the service is incredibly easy - it's just a Java interface.

It has one major restriction:

 * Since Trivet uses Java serialization, it can only be used for Java<->Java communication.

This RPC module is not intended to be a wholesale replacement for REST/JSON. However, it is incredibly convenient when
developing a Java-based Service Oriented Architecture with multiple layers. In particular, having full nested stacktraces
tends to massively reduce debugging time in a multi-layer architecture; no more digging through logs on N systems to
find the root cause of a failure.

This module depends on and installs `WebModule`; you don't need to explicitly register it with the Injector
or specify the gwizard-web maven dependency.

## Maven

Install the gwizard bom, then:

```xml
	<dependency>
		<groupId>org.gwizard</groupId>
		<artifactId>gwizard-rpc</artifactId>
	</dependency>
```

## Usage

[Example code is here](src/test/java/org/gwizard/rpc/example/)

First, create a Java interface and implementation of that interface:

```java
@ImplementedBy(HelloImpl.class)
public interface Hello {
	String hi(String name);
}
```

```java
@Remote
public class HelloImpl implements Hello {
	@Override
	public String hi(String name) {
		return "Hello, " + name;
	}
}
```

The only special flag here is that the implementation class must be flagged as `@Remote`. By default all interfaces
may be used remotely; if you wish to restrict to a subset of interfaces, you can specify them explicitly:
`@Remote(Hello.class)`. Note that `@ImplementedBy` is a standard guice annotation; it is optional and can be used
in liu of explicitly binding `Hello` to `HelloImpl` in your guice module.

The server is a standard GWizard application. In this example, the default enpdoint of `/rpc` is used:

```java
public class ServerExample {
	public static void main(String[] args) throws Exception {
		Guice.createInjector(new RpcModule()).getInstance(Run.class).start();
	}
}
```

The interface (`Hello.java`) should be packaged in a separate JAR that is available to clients. Your client code can
then look like this:

```java
public class ClientExample {
	public static void main(String[] args) throws Exception {
		Hello hello = Client.create("http://localhost:8080/rpc", Hello.class);
		String result = hello.hi("world");

		System.out.println(result);
	}
}
```

This will print "Hello, world".

## Considerations

 * Method binding is performed by calling Guice `getInstance()` on the interface class specified by the client.
 * Server security requires that only an interface can be specified (not a concrete class) and that the
   instance returned by `getInstance()` must be annoted `@Remote`.
 * Parameters and return values are encoded with Java serialization. It is **strongly recommended** that you
   specify a constant `serialVersionUID` on all classes that pass through RPC boundaries.
 * With a constant `serialVersionUID`, data model mismatches are flexible - fields not present in the
   data will be null and data for which there is no field is ignored. This is very similar to what happens with
   most JSON data mappers.
 * Trivet is clever about handling stacktraces when nested exception classes are not present on the client JVM.
 * The `RpcModule` relies on the Jetty web stack set up by the `WebModule`. You can configure the web module
   as per the instructions for that module.
 * You can change the endpoint for rpc like this: `new RpcModule("/some/other/rpc/enpdoint")`
 * There is no inherent security model in this stack. Good candidates are:
   * Use a servlet filter
   * Apply an aspect to `@Remote` classes via Guice

