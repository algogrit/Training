# Namespace Examples

Use namespaces as isolation boundaries, not as a replacement for Task Queues.

| Scenario | Namespace Shape |
|---|---|
| Dev, staging, production | `orders-dev`, `orders-staging`, `orders-prod` |
| Strict tenant isolation | One namespace per regulated tenant |
| Shared SaaS tenants | One namespace per environment, tenant ID in Search Attributes |
| Different retention needs | Separate namespace when retention policy differs |

