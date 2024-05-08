# GWizard Config

Loads a YAML file into a configuration POJO, binding it in singleton scope. You can override values using
system properties.

## Maven

Install the gwizard bom, then:

```xml
    <dependency>
        <groupId>org.gwizard</groupId>
        <artifactId>gwizard-config</artifactId>
    </dependency>
```

## Usage

[A self-contained example](src/test/java/org/gwizard/config/example/ConfigModuleExample.java)

Instantiate the `ConfigModule` by passing it a config File (typically `args[0]`) and a simple POJO class to read.

```java
File configFile = new File("somefile.yml");
ConfigModule configModule = new ConfigModule(configFile, MyConfig.class);
Injector injector = Guice.createInjector(configModule);
```

Your config object will be available in singleton scope.

The config object should be a simple POJO with getters and setters. Jackson's YAML feature will be used to
load the data; the ObjectMapper will be drawn from the injector so you can customize it with a provider if
you wish.

### Overrides

Values can be overridden by setting system properties. Since GWizard uses the dropwizard-configuration
library to load this file, [the same documentation](http://dropwizard.io/manual/core.html#configuration)
applies. This is the relevant section, adapted for GWizard's default prefix of "gw":

> You can override configuration settings by passing special Java system properties when starting your application. Overrides must start with prefix `gw.`, followed by the path to the configuration value being overridden.
> For example, to override the 'level' property inside a 'logging' property, you could start your application like this:
>
> `java -Dgw.logging.level=DEBUG`
>
> This will work even if the configuration setting in question does not exist in your config file, in which case it will get added.
> You can override configuration settings in arrays of objects like this:
>
> `java -Dgw.server.applicationConnectors[0].port=9090`
>
> You can override configuration settings in maps like this:
>
> `java -Dgw.database.properties.hibernate.hbm2ddl.auto=none`
>
> You can also override a configuration setting that is an array of strings by using the ‘,’ character as an array element separator. For example, to override a configuration setting myapp.myserver.hosts that is an array of strings in the configuration, you could start your service like this:
>
> `java -Dgw.myapp.myserver.hosts=server1,server2,server3`
>
> If you need to use the ‘,’ character in one of the values, you can escape it by using ‘,’ instead.
> The array override facility only handles configuration elements that are arrays of simple strings. Also, the setting in question must already exist in your configuration file as an array; this mechanism will not work if the configuration key being overridden does not exist in your configuration file. If it does not exist or is not an array setting, it will get added as a simple string setting, including the ‘,’ characters as part of the string.

The prefix can be changed by passing an extra parameter to `ConfigModule`:

```java
// Now set properties like this: java -Dartichoke.some.property
ConfigModule configModule = new ConfigModule(configFile, MyConfig.class, "artichoke");
```

## Best Practices

It is generally handy to put all of your application configuration in a single file. This means that different modules
which have no knowledge of each other need to somehow share space in your configuration class. This is easy to do with
provider methods in your Guice module(s).

### Creating a new configuration POJO

Inside your module, you create a POJO

```java
@Data
public class MyModuleConfig {
    private String bindAddress;
    private int port;
}
```

Then, inside your module you inject that POJO wherever it's needed.

```java
@Singleton
public class MyService {

    private final MyModuleConfig cfg;

    @Inject
    public MyService(MyModuleConfig cfg) {
        this.cfg = cfg;
    }
}
```

When you write test cases, [Guice's JIT binding](https://github.com/google/guice/wiki/JustInTimeBindings)
will take care of provisioning the POJO.

When your module is consumed by an application in their configuration (see below), the
application will have defined a Guice Provider. That binding will override the JIT binding
and rather than a new instance of the configuration POJO, you'll get the MyModuleConfig object
which is a delegate of the application's configuration.

### Consuming configuration POJOs

For example, let's say you wish to use the `WebModule` (which looks for a bound `WebConfig`) and the `HibernateModule`
(which looks for a bound `DatabaseConfig`):

```java
@Data
public class MyConfig {
    private WebConfig web;
    private MyModuleConfig myModule;
    private DatabaseConfig database;
    private String myOtherConfigProperty;
}
```

```java
public class MyModule extends AbstractModule {
    @Override
    protected void configure() { /* ... */ }

    @Provides
    public WebConfig webConfig(MyConfig cfg) {
        return cfg.getWeb();
    }

    @Provides
    public DatabaseConfig databaseConfig(MyConfig cfg) {
        return cfg.getDatabase();
    }
}
```

You have one single config file and you have bound the `WebConfig` and `DatabaseConfig` objects so that components
in modules which depend on these bindings can find them.
