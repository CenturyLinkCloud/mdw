---
permalink: /docs/features61/
title: MDW 6.1 New Features
---

## Major Enhancements
  1. "Pure Spring Boot" mode
  2. [MDW Authentication]() via JWT
  3. Full [Docker](../guides/docker.md) support, with the MDW 6.1 image available from Docker Hub
  4. [Hyperion]() platform integration 
  5. [MDW Mobile](../guides/mdw-mobile/) on Google Play and the App Store
  6. [CLI](../getting-started/cli/) Swagger codegen with automatic orchestration flow creation
  7. Kotlin language support
  8. SendGrid email notifications
  9. "Bootstrap" mode enables startup without local assets
  10. Refined Slack channel workflow collaboration
  11. [Declarative REST API service paths]() for workflow processes.
  
## Compatibility Notes
  1. Asset package metafiles saved as package.yaml instead of package.json (json metafiles still supported).
  2. Any customized application-context.xml needs to be updated to remove obsolete JAXB objects.
  3. Process lookups and launch services now require **fully-qualified** asset paths.
  4. The MDW Cloud Foundry buildpack is discontinued and replaced by [Spring Boot](../guides/spring-boot/).
  5. HTTP status codes are now standardized (no longer returning -1 for server errors).
  6. **All** service access requires authentication by default (unless excluded in access.yaml).
  7. Non-engine DB [autocommit](https://github.com/CenturyLinkCloud/mdw/issues/330) defaults to `true`.
  8. A [Designer](../getting-started/install-designer/) upgrade is required with this release.
  9. Minor DB schema changes are covered through an in-place upgrade script (no migration required).
  10. File-system based asset archiving is replaced by ASSET_REF Git commit references.
  11. Script access to masterRequestId is through runtimeContext.
  12. Customize service error responses through a ServiceMonitor.
  13. Deprecated Swagger validators must be replaced with references to new implementation.
  14. Due to MariaDB4j upgrade, embedded db data directories should be deleted/recreated.
  15. Various deprecated classes and methods removed (compiler will find these).
  16. EventManager API replaced by EventServices (accessed via ServiceLocator).
  17. PoolableAdapterBase is now deprecated in favor of TextAdapterActivity.
