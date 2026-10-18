# Clawfare 🦅

Airfare finder CLI. Searches multiple sources for flight deals, tracks prices, manages trip investigations.

## Features

- **Trip management** — Create and track flight searches over time
- **Multi-source search** — Google Flights, ITA Matrix, Skyscanner, direct airlines
- **Smart filtering** — Configurable airline allow/block lists by region
- **Price tracking** — SQLite storage with price history
- **Market arbitrage** — Compare prices across UK/US/EU/JP markets

## Installation

```bash
# From source
./gradlew nativeCompile
cp build/native/nativeCompile/clawfare ~/.local/bin/

# Or download pre-built binary
curl -L https://github.com/dsilva/clawfare/releases/latest/download/clawfare-linux-amd64 -o clawfare
chmod +x clawfare
```

## Usage

```bash
# Create a new trip
clawfare trip new tokyo-birthday \
  --from LHR,LGW \
  --to NRT,HND \
  --depart 2026-05-14 \
  --return 2026-05-30 \
  --class business

# List trips
clawfare trip list

# Show trip details and best flights
clawfare trip show tokyo-birthday

# Add a flight candidate manually
clawfare flight add tokyo-birthday \
  --airline JL \
  --price 4376 \
  --currency GBP \
  --link "https://google.com/travel/flights/..."

# Refresh prices for tracked flights
clawfare flight refresh tokyo-birthday

# Show price history
clawfare flight history tokyo-birthday
```

## Configuration

Create `~/.config/clawfare/config.yaml`:

```yaml
rules:
  allowed_regions:
    - european
    - japanese
    - american
    - korean
  blocked_airlines:
    - CA  # Air China
    - MU  # China Eastern
    - TK  # Turkish
  max_stops: 1

database:
  path: ~/.local/share/clawfare/clawfare.db
```

## Flight search web app

One command serves the search UI and the API:

```bash
./gradlew run --args="serve"
# http://localhost:8080
# ./gradlew run --args="serve --port 9090"
# CLAWFARE_PORT=9090 ./gradlew run --args="serve"
```

The home page loads the saved December 2026 Florida sample (no API call): a long-haul grid, a date matrix, a full itinerary table, and the Tampa–Mexico City seller comparison. Filters run in the browser. Every option has a Google Flights link, plus airline sites where we know them. Export writes a self-contained HTML snapshot of the current filters, or a CSV.

### Environment

Keys are read on the server only. They are not returned by `/api/status`, not put in the page, and not stored in the cache. See `.env.example`.

| Variable | What it does |
| --- | --- |
| `SERPAPI_API_KEY` | Google Flights through SerpApi. A return lookup uses `departure_token`; seller offers use `booking_token`. |
| `SEARCHAPI_API_KEY` | Google Flights through SearchAPI. Same offer shape, separate quota. |
| `TRAVELPAYOUTS_API_TOKEN` | Cached calendar fares, one call per origin × destination × month. Rows are labelled **cached**, not a live price. |
| `DUFFEL_ACCESS_TOKEN` | Duffel test mode. Every offer is labelled **mock** and is left out of the cheapest highlight. A token that is not `duffel_test_…` is still labelled mock. |
| `CLAWFARE_CACHE` | Disk cache. Default `~/.local/share/clawfare/cache`. |
| `CLAWFARE_SEARCHES` | Saved searches. Default `~/.local/share/clawfare/searches`. |
| `CLAWFARE_DB` | SQLite file shared with the CLI. Default `~/.local/share/clawfare/clawfare.db`. |

Estimate a search before running it. The page shows how many calls are already on disk, how many would hit the network, and an upper bound on return lookups. Nothing is fetched until you confirm. “Cache only” re-renders from disk. “Check quota” calls the account endpoint (SerpApi and SearchAPI), which does not spend a flight search, and shows remaining calls when the API reports them. Out of credits stops that provider, keeps the responses already cached, and does not cache the error.

A cached Google Flights row was a real fare when it was fetched (`live` price, `cached` label, with the fetch time). Travelpayouts stays `cached`. Duffel stays `mock`.

### Tests

```bash
./gradlew test
```

Tests use the sample fixtures and scripted HTTP. They do not call SerpApi, SearchAPI, Travelpayouts, or Duffel.

## Data Storage

SQLite database at `~/.local/share/clawfare/clawfare.db`:

- **trips** — Trip definitions (origins, destinations, dates, class)
- **flights** — Candidate flights with full itinerary
- **prices** — Price history for each flight

## License

MIT
