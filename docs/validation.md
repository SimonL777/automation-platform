# Validation — 2026-09-18

- Java 21 / Spring Boot local tests: 15 passed. Unit/contract database mode is H2.
- Real deployment: Docker Compose on Linux x86_64 with managed Supabase PostgreSQL via TLS session pooler.
- Deployment topology: two Supabase resources, four independently configured repositories; shared demo resources are logical separation, not four physical privilege boundaries.
- Operator positive flow: requirement → structured Workflow → deterministic Spec → actual Playwright container → private Storage report → cleanup.
- Deliberate assertion failure: failed result persisted and container removed; no duplicate run on idempotent submission.
- Supabase user flow: verified JWT identity, immutable Workspace material, owner-scoped automation/execution, Storage download, result linkage; cross-owner read denied.
- Model API: mock JSON and SSE tested; unauthorized model rejected. No live upstream model call is claimed.
- Runtime changes verified on the Docker host: readable non-root artifacts, bounded container policy and explicit completion/cleanup states.

## Not claimed

Real model-generation quality, public hostile-code isolation, distributed exactly-once execution, production capacity, live Git snapshot execution, and full UI form-submission automation are not part of this acceptance. The browser login page was rendered; the authenticated full chain was verified through its actual APIs. Private deployment addresses, credentials and run IDs remain outside the repository.
