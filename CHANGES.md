# GWizard Release Notes

# 1.1.5
2024-09-21

* `ConfigModule` is now a generic class, and type `<T>` is the config file class. 

# 1.1.4
2024-09-20

* Added import feature to config files; you can now have multiple config files.

# 1.1.3
2024-09-10

* Improvements to gwizard-test (still experimental)
  * Uses spring mock servlet objects instead of homegrown ones
  * GuiceWebExtension separated out from GuiceExtension

# 1.1.2
2024-09-07

* Improved gwizard-test to allow servlet modules to be used in tests.

# 1.1.1
2024-07-07

* Added an experimental gwizard-test module to make integration-type tests easier.

# 1.1
2024-06-23

* Configuration bindings are now interfaces. Concrete properties classes are provided for 
your convenience; for example, `WebConfigProperties` implements `WebConfig`.
* `gwizard-rest` includes an optional `ThrowableMapper` which presents a useful
error format. See the javadoc.
* `gwizard-rpc` now is based on Trivet 2.0, which has an improved (but incompatible)
serialization format.