SLF4J Bundles
=============

SLF4J is available as a set of OSGi bundles in the Orbit repository.
However, the packaging as some special requirements because of the
way the SLF4J API integrates with the actual logging implementation.
Therefore, the following bundle layout was created.


org.slf4j.api
-------------

This is the API bundle. It has no dependencies (neither Require-Bundle
nor Import-Package) it exports the API packages. The actual
implementation must be contributed as a fragment to this bundle.

Note, it may have been possible to have an Import-Package dependency on
the implementation packages. However, this approach was discarded because
it would have introduced binary (or even worse - source) cycles in a
workspace which has both - the API and the implementation bundle.

An Eclipse-GenericRequire header is used in order to specify a dependency
on a SLF4J logger implementation. Note, the GenericRequire header will try
to align with the SLF4J API compatibility list as found in LoggerFactory.


org.slf4j.impl.<impl-name>
--------------------------------

This defines a bundle for a SLF4J implementation library. Every
implementation bundle must be a fragment of the API bundle
(Fragment-Host: org.slf4j.api) _and_ provide a SLF4J logger implementation
(Eclipse-GenericCapability: org.slf4j.impl.StaticLoggerBinder) header.
Special care must be taken to the version dependency as SLF4J allows for
breaking changes within the implementation between service releases.
The best practise would be specifying a very strict version range.

Example:
  Fragment-Host: org.slf4j.api;bundle-version="[a.b.0,a.b+1.0)"
  Eclipse-GenericCapability: org.slf4j.impl.StaticLoggerBinder; version="a.b.c"


