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

## Data Storage

SQLite database at `~/.local/share/clawfare/clawfare.db`:

- **trips** — Trip definitions (origins, destinations, dates, class)
- **flights** — Candidate flights with full itinerary
- **prices** — Price history for each flight

## License

MIT
