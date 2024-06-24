# GWizard Release Notes

# 1.1
2024-06-23

* Configuration bindings are now interfaces. Concrete properties classes are provided for 
your convenience; for example, `WebConfigProperties` implements `WebConfig`.
* `gwizard-rest` includes an optional `ThrowableMapper` which presents a useful
error format. See the javadoc.
* `gwizard-rpc` now is based on Trivet 2.0, which has an improved (but incompatible)
serialization format.