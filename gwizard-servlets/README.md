# GWizard Servlets

Adds some helpful administrative/ops servlets to your instance, that can help give you a better view of
your server. It is very close to the dropwizard-servlets modules, however continues with the ongoing theme
of staying in Guice's warm embrace.

## Maven

```xml
	<dependency>
		<groupId>org.gwizard</groupId>
		<artifactId>gwizard-servlets</artifactId>
		<version>${gwizard.version}</version>
	</dependency>
```

## Usage

You can use each module independently of use the *master* AdminServlet which is a helpful servlet
that bundles all the other ones.

It is **important** to make sure you add any of the modules **before** you add the RestModule.

The quick and easy way to get-going is simply to add the AdminServletModule to your injector


    Injector injector = Guice.createInjector(
                new NropModule(),
                new ConfigModule(configFile, NropConfig.class),
                new LoggingModule(),
                new AdminServletModule(),
                new RestModule(),
                new MetricsModule(),
                new HealthChecksModule());
                
The AdminServletModule will make an endpoint available at `/admin` and all other servlets nested within the same URI.

If you want to pick and chose, simply install each servlet's module (befoe the RestModule) and provide any prefix you wish.