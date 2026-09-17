# Automation Platform

A Java automation delivery service: requirement → validated Workflow → deterministic Spec → Sandbox execution → report, with optional asynchronous Git source snapshots.

## Scope

- Durable owner-scoped Run tasks with idempotency and lease fencing.
- Model Gateway capability call or an explicitly supplied validated Workflow.
- A versioned, restricted Workflow contract and deterministic Java compiler.
- Real Playwright browser execution in the Sandbox Runner image.
- Workflow, Spec and report artifacts, compiler/hash provenance and Model/Sandbox correlation IDs.
- Registered HTTPS Git repositories only; async clone/fetch/checkout, resolved commit SHA and workspace cleanup.

v0.1 intentionally targets one synthetic Todo page. It is not a general browser exploration agent, visual workflow editor, complete test-management system or arbitrary code runner. Git tasks snapshot registered repositories; they do not execute code from a cloned repository or push commits. Generated test behavior stays in the constrained Workflow contract.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/runs` | Requirement or Workflow, optional workspace/reference; `Idempotency-Key` required |
| GET | `/api/runs` | List owner-scoped runs |
| GET | `/api/runs/{id}` | Result and provenance |
| POST | `/api/runs/{id}/cancel` | Cancel orchestration and request child cancellation |
| POST | `/api/compile` | Validate and compile a Workflow without execution |
| GET | `/api/artifacts/{id}/{name}` | Owner-scoped artifact download |
| GET | `/api/repositories` | Registered repository keys |
| POST | `/api/repository-tasks` | Async Git snapshot for a registered key/ref |
| GET | `/api/repository-tasks/{id}` | Git task result |

Dependency configuration:

```text
MODEL_GATEWAY_URL=http://model-gateway:8082
MODEL_SERVICE_TOKEN=<model service credential>
SANDBOX_GATEWAY_URL=http://sandbox-gateway:8081
SANDBOX_SERVICE_TOKEN=<sandbox service credential>
GIT_REPOSITORIES_JSON={"example":"https://github.com/OWNER/PUBLIC_REPO.git"}
```

## Compiler and Runner

`contracts/workflow-v1.schema.json` describes the accepted shape. Java and Runner validators enforce the operational constraints (including a real nonblank assertion). Operations are `fill(title)`, `click(add)` and `assertText(items)`. Values are serialized as data, never inserted as raw JavaScript.

The compiler emits a reproducible Spec fragment and hash. The Runner reconstructs that fragment with the same compiler version from the validated Workflow and executes it against its embedded page; the orchestration result verifies the resulting hash. This avoids accepting arbitrary caller-provided JavaScript. Browser actions are real; the target application is synthetic.

```bash
cd runner
npm ci
npm test
# Browser/container execution is a separate runtime validation step.
docker build -t ai-sdlc-runner:0.1.0 .
```

## Java/Git design

The HTTP request persists a task and returns quickly. A bounded scheduled worker claims work in a short database transaction. External Git/model/container calls run outside that transaction. Each Git task gets a temporary workspace, safe argument arrays, disabled credential prompting/hooks/local protocols, deadlines and output caps. Resolved commit SHA is recorded. No fifth Git microservice is needed.

The first worker implementation processes one task at a time per instance; it is bounded by construction. Horizontal workers rely on database CAS fencing, not `@Async` alone. Automatic replay of uncertain side effects is deliberately absent; expired work is marked `LOST` for explicit recovery.

## Implementation status

Local Java tests: **15 passed** on 2026-09-18, using the explicit H2 test profile. This is not PostgreSQL/Supabase, Docker or NAS acceptance. Four-project data isolation is configured; real project credentials and NAS login are still required for deployment. Public GitHub publication remains pending.

## Java development

Requires JDK 21 and Maven 3.9+. Default startup requires a real PostgreSQL/Supabase configuration. For an explicit local-only development instance:

```bash
export API_TOKEN="$(python3 -c 'import secrets; print(secrets.token_urlsafe(36))')"
export SERVICE_TOKEN="$(python3 -c 'import secrets; print(secrets.token_urlsafe(36))')"
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

`local` opts into file-backed H2 in PostgreSQL compatibility mode and local artifacts. It does not validate PostgreSQL concurrency, Supabase RLS/Storage or a deployment. No production fallback to H2 occurs. `API_TOKEN` is for a private single-operator workspace; optional Supabase JWT auth verifies issuer, signature and audience and uses the user subject as resource owner. Only a separate service credential may forward `X-Owner-Id`.

For production copy `.env.example` to private configuration, use Supabase PostgreSQL and Storage, and set a dedicated database schema. Flyway owns this repository's schema. Java JDBC authorization is explicit; do not assume browser RLS applies to a privileged JDBC connection. Artifact requests check task ownership before reading Storage. Never put service-role keys in a browser.

When ready to validate:

```bash
mvn verify
```

Tests cover idempotency, ownership, concurrent claiming, cancelled/stale completion, artifact paths, deterministic compilation and bounded child processes. H2 tests are not PostgreSQL integration proof.

## Integrated NAS deployment

Keep the four projects as siblings and use `ai-sdlc/examples/stack/`. Use a separate Supabase project for each platform; the integration configuration keeps their database and privileged credentials separate. Only the workbench reverse proxy is host-published. The integration README separates mock-model, real-provider, local-development and Supabase modes.

## License

Apache-2.0. All examples are synthetic. This is an independent implementation, not a redistribution of an employer's platform or data.
