# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is the **SO ETSI SOL005 Adapter** — a Spring Boot microservice within ONAP Service Orchestration (SO) that adapts VFC (Virtual Function Controller) NS lifecycle management calls to ETSI SOL005-compliant NFVOs. It supports dual-path routing: legacy VFC-style and SOL005-compliant requests, selected at runtime based on request parameters and `InstanceNfvoMapping` database records.

## Build Commands

```bash
mvn clean install                     # Full build with tests
mvn clean install -DskipTests         # Build without tests
mvn test                              # Run all tests
mvn test -pl so-etsi-sol005-adapter-application   # Tests for the application module only
mvn test -pl so-etsi-sol005-adapter-application -Dtest=VfcManagerSol005Test   # Single test class
mvn clean install -Pdocker            # Build with Docker image
```

Requires ONAP `settings.xml` at `~/.m2/settings.xml` to resolve `org.onap.so:mso-requests-db` and other SO parent artifacts from ONAP Nexus.

## Architecture

### Module Structure

- **so-etsi-sol005-adapter-application** — The Spring Boot application (main class: `MSOVfcApplication`). Contains all business logic.
- **packages/docker** — Docker image build (activated with `-Pdocker`). Produces `onap/so/so-etsi-sol005-adapter`.

### Request Flow

REST endpoint: `VfcAdapterRest` at `/v1/vfcadapter`

Routing logic in `VfcAdapterRest` decides between two managers:
- **`VfcManager`** — Legacy VFC NFVO integration (URLs like `/api/nslcm/v1/ns/...`)
- **`VfcManagerSol005`** — SOL005-compliant NFVO integration (URLs like `/api/nslcm/v1/ns_instances/...`)

Route selection:
- For **create/instantiate/scale**: presence of `isSol005Interface` key in `additionalParamForNs` triggers SOL005 path
- For **delete/terminate/query**: existence of an `InstanceNfvoMapping` DB record for the nsInstanceId/jobId triggers SOL005 path

### Key Differences Between Paths

| Aspect | VfcManager (legacy) | VfcManagerSol005 |
|--------|-------------------|------------------|
| NS instance ID field | `nsInstanceId` | `id` |
| Job ID source | Response body `jobId` | `Location` header (URI parsed) |
| NFVO URL resolution | Hardcoded URL patterns | AAI lookup → `InstanceNfvoMapping` DB |
| Job status model | `NsProgressStatus` / `ResponseDescriptor` | `NsLcmOpOcc` / `operationState` enum |

### External Dependencies

- **MariaDB** (`requestdb`) — stores `ResourceOperationStatus`, `OperationStatus`, `InstanceNfvoMapping`
- **AAI** — looked up via `RestfulUtil.getNfvoFromAAI()` to resolve NFVO endpoint details
- **NFVO** — the actual SOL005-compliant Network Function Virtualization Orchestrator

### Configuration

- `application.yaml` — production config; uses env vars `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`
- `application-test.yaml` — test profile; uses embedded MariaDB4j on port 3307
- Server runs on port 8080; actuator at `/manage`

### Testing

Tests use embedded MariaDB (`EmbeddedMariaDbConfig`) and Mockito. `VfcManagerSol005Test` and `VfcManagerTest` are unit tests with mocked repositories and `RestfulUtil`. `WireMock` is available for integration-level tests.

Java 11 target. JUnit 4 with `SpringRunner`.
