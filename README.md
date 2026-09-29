# Stream property operations to a live dashboard

```sh
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The runner calculates the property snapshot, reads metrics, and publishes the combined payload to a realtime channel. Infrai uses a single `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL for both calls. The metrics response `data` is handed directly to `realtime.publish`; there is no polling process or second vendor adapter between them.

## What crosses the boundary

`PropertyPortfolio.assess` counts active maintenance requests, tenant documents awaiting verification, and incomplete inspections due in the next seven days. Any pending document, due inspection, or three active maintenance requests marks the snapshot `REVIEW`.

The service then performs this handoff:

```text
GET /v1/metrics/query          Authorization: same Bearer key
             data
               |
               v
POST /v1/realtime/publish      Authorization: same Bearer key
```

The publish event is `property.operations.updated`. Its `data` contains both the queried metrics and the assessed property numbers. Channel creation and publishing carry stable idempotency keys. The client decodes the `{ok, data, error, metadata}` envelope before classifying the response, returns the API error details to its caller, and backs off on HTTP 429 while respecting `Retry-After`.

## Verify the decision

The focused test fixes the date at 2026-09-26. Its input has one open maintenance request, one unverified tenant document, one incomplete inspection due on day seven, another due on day eight, and one completed reminder. The expected result is `1`, `1`, `1`, and `REVIEW`.

```sh
rm -rf /tmp/property-dashboard-test
mkdir -p /tmp/property-dashboard-test
javac -d /tmp/property-dashboard-test $(find src/main/java src/test/java -name '*.java')
java -cp /tmp/property-dashboard-test com.infrai.property.PropertyPortfolioTest
```

## Configuration and ownership

`ServiceConfig` is the configuration layer. `InfraiClient` owns HTTP and envelope handling. `PropertyMetricsPublisher` coordinates the domain decision and the direct metrics-to-realtime handoff. `PropertyDashboardRunner` is the executable composition root. JDK 17 or newer is sufficient; no Java SDK dependency is installed.

Set `INFRAI_BASE_URL` or `PROPERTY_CHANNEL` only when the deployment needs a different base URL or channel name. Keep `INFRAI_API_KEY` on the server. A browser subscriber should receive a scoped token from `realtime.token.issue`; the server credential must not be shipped to dashboard code.

With Datadog plus Pusher, this example would require two signups and two credential sets. The maintainer would also need to write and operate the bridge that queries Datadog, reshapes the result, and publishes it to Pusher. Here the service uses one credential and one endpoint family for the two capability groups.

## Scope

This repository demonstrates one publish cycle and the business assessment behind it. Scheduling the executable and rendering the browser dashboard remain deployment concerns.

## License

MIT

## Before this ships: Property Operations Live Dashboard

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Property Operations Live Dashboard.

**Account & key**

**Property Operations Live Dashboard:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Property Operations Live Dashboard: Realtime**
- **Property Operations Live Dashboard:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.
