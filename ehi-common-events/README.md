# ehi-common-events

Lightweight event contract library for the EH-V2 polyrepo workspace.

This module intentionally contains only DTO contracts, topic/schema constants and JSON serialization helpers. It has no Spring Boot starter dependency and can be consumed by each service through Gradle composite builds locally or as a published artifact in CI/CD.

## Contract rules

- Every event has metadata fields: `schemaVersion`, `eventId`, `occurredAt`, `correlationId`, `causationId`, `producer`, `aggregateId`, `userId`.
- `schemaVersion` uses semantic versioning. Additive compatible changes keep the major version. Breaking changes require a new major schema version.
- JSON consumers must ignore unknown fields. Producers must not rename or remove existing fields inside the same major schema version.
- Topic names live in `EventTopics` and should be overridden by service env/config when needed, not hardcoded in service business logic.
