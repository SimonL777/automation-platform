# Workflow v2

Version 1 remains readable and compiles with compiler 0.1.0. Version 2 compiles with 0.2.0 and uses registered target identifiers: `web:todo-demo`, `api:todo-demo`, or an operator-configured profile key.

Web locators: `testId:...`, `css:...`, `text:...`, `role:button|Name`. Supported operations: navigate (same-origin path), fill, click, check, select, assertText, assertVisible, assertCount. Values are serialized as data and never inserted as raw source.

API operations: request (target is HTTP method, value is a JSON string with path/body/headers), assertStatus, assertJson (simple $.field[0] path with JSON-encoded expected value), assertBody. A request must precede an API assertion. Arbitrary origins, redirects and caller-supplied Authorization/Cookie/Host headers are not allowed in the Workflow.

Every Workflow needs at least one assertion. Every case edit creates an immutable revision with a Workflow hash. Every actual run records the source revision, compiler version and Spec hash.

`SANDBOX_TARGETS_JSON` is server configuration mapping profile keys to trusted base URLs. The default `todo-demo` starts an isolated local fixture inside the Runner. External targets must be explicitly registered; the browser remains restricted to the registered origin and API redirects are disabled. Authentication profiles and cross-origin application flows are later extensions, not implicit privileges.
