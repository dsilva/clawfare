package com.clawfare.cli

import com.clawfare.db.Database
import com.clawfare.model.*
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import java.io.File

class Clawfare : CliktCommand(
    help = "Airfare finder CLI - manage trips and track flight prices"
) {
    override fun run() = Unit
}

class TripCommand : CliktCommand(name = "trip", help = "Manage trips") {
    override fun run() = Unit
}

class TripNew : CliktCommand(name = "new", help = "Create a new trip") {
    private val slug by argument(help = "Trip identifier (e.g., tokyo-birthday)")
    private val from by option("--from", "-f", help = "Origin airports (comma-separated)").required()
    private val to by option("--to", "-t", help = "Destination airports (comma-separated)").required()
    private val depart by option("--depart", "-d", help = "Departure date (YYYY-MM-DD)").required()
    private val returnDate by option("--return", "-r", help = "Return date (YYYY-MM-DD)")
    private val cabinClass by option("--class", "-c", help = "Cabin class").default("economy")
    private val maxStops by option("--max-stops", help = "Maximum stops").int().default(1)
    
    override fun run() {
        val db = getDatabase()
        
        if (db.getTrip(slug) != null) {
            echo("Error: Trip '$slug' already exists", err = true)
            throw CliktError()
        }
        
        val trip = Trip(
            slug = slug,
            origins = from.split(",").map { it.trim().uppercase() },
            destinations = to.split(",").map { it.trim().uppercase() },
            departDate = depart,
            returnDate = returnDate,
            cabinClass = CabinClass.fromString(cabinClass),
            tripType = if (returnDate != null) TripType.ROUND_TRIP else TripType.ONE_WAY,
            maxStops = maxStops
        )
        
        db.createTrip(trip)
        
        echo("✈️  Created trip: $slug")
        echo("   From: ${trip.origins.joinToString(", ")}")
        echo("   To: ${trip.destinations.joinToString(", ")}")
        echo("   Depart: ${trip.departDate}")
        if (trip.returnDate != null) {
            echo("   Return: ${trip.returnDate}")
        }
        echo("   Class: ${trip.cabinClass}")
        echo("   Max stops: ${trip.maxStops}")
    }
}

class TripList : CliktCommand(name = "list", help = "List all trips") {
    override fun run() {
        val db = getDatabase()
        val trips = db.getAllTrips()
        
        if (trips.isEmpty()) {
            echo("No trips found. Create one with: clawfare trip new <slug>")
            return
        }
        
        echo("📋 Trips:\n")
        trips.forEach { trip ->
            val flights = db.getFlightsForTrip(trip.id)
            val flightCount = flights.size
            echo("  ${trip.slug}")
            echo("    ${trip.origins.joinToString("/")} → ${trip.destinations.joinToString("/")}")
            echo("    ${trip.departDate}${trip.returnDate?.let { " → $it" } ?: ""} | ${trip.cabinClass}")
            echo("    $flightCount flight${if (flightCount != 1) "s" else ""} tracked")
            echo()
        }
    }
}

class TripShow : CliktCommand(name = "show", help = "Show trip details and best flights") {
    private val slug by argument(help = "Trip identifier")
    
    override fun run() {
        val db = getDatabase()
        val trip = db.getTrip(slug)
        
        if (trip == null) {
            echo("Error: Trip '$slug' not found", err = true)
            throw CliktError()
        }
        
        echo("✈️  ${trip.slug}")
        echo("   Route: ${trip.origins.joinToString("/")} → ${trip.destinations.joinToString("/")}")
        echo("   Dates: ${trip.departDate}${trip.returnDate?.let { " → $it" } ?: ""}")
        echo("   Class: ${trip.cabinClass} | Max stops: ${trip.maxStops}")
        
        val flights = db.getFlightsWithPrices(trip.id)
        
        if (flights.isEmpty()) {
            echo("\n   No flights tracked yet.")
            echo("   Add one with: clawfare flight add $slug --airline XX --price 1234 --link \"...\"")
            return
        }
        
        echo("\n📊 Flights (sorted by price):\n")
        flights.forEachIndexed { idx, (flight, price, history) ->
            val priceStr = price?.let { "${it.currency} ${it.amount}" } ?: "no price"
            val airline = flight.outbound.legs.firstOrNull()?.airlineCode ?: "?"
            val stops = flight.outbound.stops
            val stopStr = if (stops == 0) "direct" else "$stops stop${if (stops > 1) "s" else ""}"
            
            echo("  ${idx + 1}. $airline $stopStr — $priceStr")
            echo("     ${flight.source} | ${flight.outbound.departAirport} → ${flight.outbound.arriveAirport}")
            if (history.size > 1) {
                val oldest = history.last()
                val change = price!!.amount - oldest.amount
                val changeStr = if (change > 0) "+${change.toInt()}" else "${change.toInt()}"
                echo("     Price history: $changeStr since ${oldest.checkedAt.take(10)}")
            }
        }
    }
}

class TripDelete : CliktCommand(name = "delete", help = "Delete a trip") {
    private val slug by argument(help = "Trip identifier")
    private val force by option("--force", "-f", help = "Skip confirmation").flag()
    
    override fun run() {
        val db = getDatabase()
        val trip = db.getTrip(slug)
        
        if (trip == null) {
            echo("Error: Trip '$slug' not found", err = true)
            throw CliktError()
        }
        
        if (!force) {
            echo("Delete trip '$slug' and all its flights? [y/N]")
            val confirm = readlnOrNull()?.lowercase()
            if (confirm != "y" && confirm != "yes") {
                echo("Cancelled.")
                return
            }
        }
        
        db.deleteTrip(slug)
        echo("Deleted trip: $slug")
    }
}

// Flight commands

class FlightCommand : CliktCommand(name = "flight", help = "Manage flights") {
    override fun run() = Unit
}

class FlightAdd : CliktCommand(name = "add", help = "Add a flight candidate") {
    private val tripSlug by argument(help = "Trip identifier")
    private val airline by option("--airline", "-a", help = "Primary airline code").required()
    private val price by option("--price", "-p", help = "Price amount").double().required()
    private val currency by option("--currency", help = "Currency code").default("GBP")
    private val market by option("--market", help = "Market (UK/US/EU/JP)").default("UK")
    private val link by option("--link", "-l", help = "Booking/share link").required()
    private val source by option("--source", "-s", help = "Source (google_flights, ita_matrix, etc)").default("manual")
    private val departTime by option("--depart-time", help = "Departure time (ISO)")
    private val arriveTime by option("--arrive-time", help = "Arrival time (ISO)")
    private val stops by option("--stops", help = "Number of stops").int().default(0)
    private val notes by option("--notes", "-n", help = "Notes")
    
    override fun run() {
        val db = getDatabase()
        val trip = db.getTrip(tripSlug)
        
        if (trip == null) {
            echo("Error: Trip '$tripSlug' not found", err = true)
            throw CliktError()
        }
        
        val now = java.time.Instant.now().toString()
        
        // Create minimal flight segment
        val outbound = FlightSegment(
            departAirport = trip.origins.first(),
            departTime = departTime ?: now,
            arriveAirport = trip.destinations.first(),
            arriveTime = arriveTime ?: now,
            durationMinutes = 0,
            stops = stops,
            legs = listOf(
                FlightLeg(
                    flightNumber = "",
                    airline = airline,
                    airlineCode = airline,
                    departAirport = trip.origins.first(),
                    departTime = departTime ?: now,
                    arriveAirport = trip.destinations.first(),
                    arriveTime = arriveTime ?: now,
                    durationMinutes = 0
                )
            )
        )
        
        val flight = Flight(
            tripId = trip.id,
            shareLink = link,
            source = source,
            outbound = outbound,
            returnSegment = null,  // TODO: support return
            cabinClass = trip.cabinClass,
            notes = notes ?: ""
        )
        
        val flightId = db.addFlight(flight)
        
        val priceRecord = Price(
            flightId = flightId,
            amount = this.price,
            currency = currency,
            market = market,
            checkedAt = now
        )
        
        db.addPrice(priceRecord)
        
        echo("✅ Added flight: $airline ${if (stops == 0) "direct" else "$stops-stop"} at $currency $price")
    }
}

class FlightRefresh : CliktCommand(name = "refresh", help = "Refresh prices for tracked flights") {
    private val tripSlug by argument(help = "Trip identifier")
    
    override fun run() {
        val db = getDatabase()
        val trip = db.getTrip(tripSlug)
        
        if (trip == null) {
            echo("Error: Trip '$tripSlug' not found", err = true)
            throw CliktError()
        }
        
        val flights = db.getFlightsForTrip(trip.id)
        
        if (flights.isEmpty()) {
            echo("No flights to refresh for '$tripSlug'")
            return
        }
        
        echo("🔄 Refreshing ${flights.size} flight(s)...")
        echo("   (Price refresh requires browser automation - not yet implemented)")
        echo("   For now, add updated prices manually with: clawfare flight price <trip> <flight-id> --amount <price>")
    }
}

class FlightHistory : CliktCommand(name = "history", help = "Show price history") {
    private val tripSlug by argument(help = "Trip identifier")
    
    override fun run() {
        val db = getDatabase()
        val trip = db.getTrip(tripSlug)
        
        if (trip == null) {
            echo("Error: Trip '$tripSlug' not found", err = true)
            throw CliktError()
        }
        
        val flights = db.getFlightsWithPrices(trip.id)
        
        if (flights.isEmpty()) {
            echo("No flights tracked for '$tripSlug'")
            return
        }
        
        echo("📈 Price history for $tripSlug:\n")
        flights.forEach { (flight, _, history) ->
            val airline = flight.outbound.legs.firstOrNull()?.airlineCode ?: "?"
            echo("  $airline (${flight.source}):")
            history.forEach { p ->
                echo("    ${p.checkedAt.take(16)} — ${p.currency} ${p.amount} (${p.market})")
            }
            echo()
        }
    }
}

class ServeCommand : CliktCommand(
    name = "serve",
    help = "Start the flight-search web UI (http://localhost:8080)"
) {
    private val port by option("--port", "-p", help = "Port (or CLAWFARE_PORT)").int().default(0)

    override fun run() {
        val chosen = when {
            port > 0 -> port
            !System.getenv("CLAWFARE_PORT").isNullOrBlank() -> System.getenv("CLAWFARE_PORT").toInt()
            else -> 8080
        }
        com.clawfare.web.Server.start(chosen)
    }
}

// Helper

internal fun defaultDatabasePath(): String {
    val home = System.getProperty("user.home")
    return System.getenv("CLAWFARE_DB") ?: "$home/.local/share/clawfare/clawfare.db"
}

private fun getDatabase(): Database = Database(defaultDatabasePath())
