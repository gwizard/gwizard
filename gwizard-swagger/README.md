# GWizard Swagger


GWizard Swagger servess a swagger configuration file generated from your JAX-RS 
resources and @Api annoted methods. At the moment, only the swagger json or yaml
file is provided and not the complete swagger-ui.

See https://github.com/swagger-api/swagger-core

## Usage

* Add the Maven dependency

```xml
	<dependency>
		<groupId>org.gwizard</groupId>
		<artifactId>gwizard-swagger</artifactId>
		<version>${gwizard.version}</version>
	</dependency>
```

* Add the Swagger configuration to your application's config

```java
	@Data
	public class ExampleConfig {
		...	
		private SwaggerConfig swagger = new SwaggerConfig();
		...
	}
```

* Add the Swagger configuration provider to your applications Guice module

```java	
	@Provides
	public SwaggerConfig swaggerConfig(ExampleConfig cfg) {
		return cfg.getSwagger();
	}
```

* Add the configuration to your applications configuration file

```
	swagger:
	  resourcePackages: com.example.app.resource
```

* Load the Swagger module in your Main method

```java
	public class Main {
		public static void main(String[] args) throws Exception {
			if (args.length < 1) {
				System.err.println("First argument needs to be a yaml config file, doofus");
				return;
			}
			Injector injector = Guice.createInjector(
					...
					new SwaggerModule());
			injector.getInstance(Run.class).start();
		}
	}
```

* Add the Swagger annotations to your resource classes and classes

* Create an interface in you base resource package for the main api documentation 

```java
	@SwaggerDefinition(
		info = @Info(
		  description = "Example Gwizard App",
		  version = "V1.0.1",
		  title = "The GWizard Example API",
		  termsOfService = "http://localhost:8081/terms.html",
		  contact = @Contact(
				name = "Gandalf", 
				email = "into.shadow@middleearth.org", 
				url = "http://middleearth.org"
		  ),
		  license = @License(
				name = "Apache 2.0", 
				url = "http://www.apache.org/licenses/LICENSE-2.0"
		  )
		),
		consumes = {"application/json", "application/xml"},
		produces = {"application/json", "application/xml"},
		schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
		tags = {
				@Tag(name = "Private", description = "Tag used to denote operations as private")
		}, 
		externalDocs = @ExternalDocs(value = "Rings", url = "http://ringsapi.io/middleearth.html")
	)
	public interface ExampleApiConfig { /* empty */ }
```

## Sample Application

[A self-contained example](src/test/java/org/gwizard/services/example/ServicesModuleExample.java)

## Notes

Swagger 2 does not need the host and port to be specified as it will use the relative url from
where the configuration is loaded from.