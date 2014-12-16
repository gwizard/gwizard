# GWizard Hibernate

Love it or hate it, you probably have had to use it. `HibernateModule` is a very thin bit of glue helping
[Guice Persist](https://github.com/google/guice/wiki/GuicePersist) give you JPA access to a relational database.
All we do is help manage the configuration; Guice Persist (and of course Hibernate) handles all the heavy lifting.

## Maven

```xml
	<dependency>
		<groupId>com.voodoodyne.gwizard</groupId>
		<artifactId>gwizard-hibernate</artifactId>
		<version>${gwizard.version}</version>
	</dependency>
```

## Usage

[A self-contained example](src/test/java/com/voodoodyne/gwizard/hibernate/HibernateModuleExample.java)

Hibernate does not fit easily into a pasted code snippet. Look at the self-contained example above, or
better yet the [full GWizard example](https://github.com/stickfigure/gwizard-example).

What you need to know:

* Database configuration is drawn from a bound `DatabaseConfig` object. There are no defaults, you must provide it.
You can configure all database and hibernate properties here.

* You must create a `persistence.xml` file and package it in your jar's `META-INF` directory. We hate this, but JPA
absolutely requires it. Fortunately, it need not contain anything other than this boilerplate:

```xml
<persistence version="2.0" xmlns="http://java.sun.com/xml/ns/persistence"
			 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			 xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd">
	<persistence-unit name="gw-persistence-unit" transaction-type="RESOURCE_LOCAL">
		<provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
	</persistence-unit>
</persistence>
```

* If for some reason you want to change the persistence name, it is an optional parameter to `HibernateModule`'s constructor.

* Read the documentation for [Guice Persist](https://github.com/google/guice/wiki/GuicePersist)! The most important
bits are understanding `@Transactional`, and if you choose to install it, `PersistFilter`. Speaking of which, you can
install that like any other filter if you are using the `WebModule` or `RestModule`.

## `EM` and scope

You are free to @Inject the `EntityManager` all over your code if you want to, but I personally think this sucks.
It's especially painful when you want to access the database from within entity POJO classes[1] or other places
where injection is not convenient. I find that aspects like transaction state and identity are best modeled as thread
locals, and so you get a convenient little helper method (which you are of course free to ignore):

```java
EM.em().persist(new Thing(...));
```
Better yet, with a static import:
```java
em().persist(new Thing(...));
```

This static method always returns the request-scoped `EntityManager`.

	[1]: If you're thinking screaming into your monitor "don't put data access code in your entity classes!", then you
	have never used polymorphic entity objects in any sophisticated capacity. You're just wrong about this. Sorry.