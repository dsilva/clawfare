package com.clawfare.db

import com.clawfare.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object Trips : LongIdTable("trips") {
    val slug = varchar("slug", 100).uniqueIndex()
    val origins = text("origins")  // JSON array
    val destinations = text("destinations")  // JSON array
    val departDate = varchar("depart_date", 10)
    val returnDate = varchar("return_date", 10).nullable()
    val cabinClass = varchar("cabin_class", 20)
    val tripType = varchar("trip_type", 20)
    val maxStops = integer("max_stops").default(1)
    val notes = text("notes").default("")
    val createdAt = varchar("created_at", 30)
    val updatedAt = varchar("updated_at", 30)
}

object Flights : LongIdTable("flights") {
    val tripId = long("trip_id").references(Trips.id)
    val shareLink = text("share_link")
    val sourceProvider = varchar("source", 50)
    val outbound = text("outbound")  // JSON
    val returnSegment = text("return_segment").nullable()  // JSON
    val cabinClass = varchar("cabin_class", 20)
    val cabinMixed = bool("cabin_mixed").default(false)
    val notes = text("notes").default("")
    val tags = text("tags").default("[]")  // JSON array
    val createdAt = varchar("created_at", 30)
}

object Prices : LongIdTable("prices") {
    val flightId = long("flight_id").references(Flights.id)
    val amount = double("amount")
    val currency = varchar("currency", 3)
    val market = varchar("market", 10)
    val checkedAt = varchar("checked_at", 30)
}

class Database(dbPath: String) {
    private val json = Json { ignoreUnknownKeys = true }
    
    init {
        // Ensure parent directory exists
        File(dbPath).parentFile?.mkdirs()
        
        org.jetbrains.exposed.sql.Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC"
        )
        
        transaction {
            SchemaUtils.create(Trips, Flights, Prices)
        }
    }
    
    // ===== Trips =====
    
    fun createTrip(trip: Trip): Long = transaction {
        val now = java.time.Instant.now().toString()
        Trips.insertAndGetId {
            it[slug] = trip.slug
            it[origins] = json.encodeToString(trip.origins)
            it[destinations] = json.encodeToString(trip.destinations)
            it[departDate] = trip.departDate
            it[returnDate] = trip.returnDate
            it[cabinClass] = trip.cabinClass.toString()
            it[tripType] = trip.tripType.toString()
            it[maxStops] = trip.maxStops
            it[notes] = trip.notes
            it[createdAt] = now
            it[updatedAt] = now
        }.value
    }
    
    fun getTrip(slug: String): Trip? = transaction {
        Trips.selectAll().where { Trips.slug eq slug }
            .firstOrNull()
            ?.toTrip()
    }
    
    fun getAllTrips(): List<Trip> = transaction {
        Trips.selectAll()
            .orderBy(Trips.updatedAt, SortOrder.DESC)
            .map { it.toTrip() }
    }
    
    fun deleteTrip(slug: String): Boolean = transaction {
        val trip = getTrip(slug) ?: return@transaction false
        // Delete associated prices and flights first
        val flightIds = Flights.selectAll().where { Flights.tripId eq trip.id }.map { it[Flights.id].value }
        flightIds.forEach { fid ->
            Prices.deleteWhere { flightId eq fid }
        }
        Flights.deleteWhere { tripId eq trip.id }
        Trips.deleteWhere { Trips.slug eq slug } > 0
    }
    
    private fun ResultRow.toTrip() = Trip(
        id = this[Trips.id].value,
        slug = this[Trips.slug],
        origins = json.decodeFromString(this[Trips.origins]),
        destinations = json.decodeFromString(this[Trips.destinations]),
        departDate = this[Trips.departDate],
        returnDate = this[Trips.returnDate],
        cabinClass = CabinClass.fromString(this[Trips.cabinClass]),
        tripType = TripType.valueOf(this[Trips.tripType]),
        maxStops = this[Trips.maxStops],
        notes = this[Trips.notes],
        createdAt = this[Trips.createdAt],
        updatedAt = this[Trips.updatedAt]
    )
    
    // ===== Flights =====
    
    fun addFlight(flight: Flight): Long = transaction {
        val now = java.time.Instant.now().toString()
        Flights.insertAndGetId {
            it[tripId] = flight.tripId
            it[shareLink] = flight.shareLink
            it[sourceProvider] = flight.source
            it[outbound] = json.encodeToString(flight.outbound)
            it[returnSegment] = flight.returnSegment?.let { seg -> json.encodeToString(seg) }
            it[cabinClass] = flight.cabinClass.toString()
            it[cabinMixed] = flight.cabinMixed
            it[notes] = flight.notes
            it[tags] = json.encodeToString(flight.tags)
            it[createdAt] = now
        }.value
    }
    
    fun getFlightsForTrip(tripId: Long): List<Flight> = transaction {
        Flights.selectAll().where { Flights.tripId eq tripId }
            .map { it.toFlight() }
    }
    
    fun getFlight(id: Long): Flight? = transaction {
        Flights.selectAll().where { Flights.id eq id }
            .firstOrNull()
            ?.toFlight()
    }
    
    private fun ResultRow.toFlight() = Flight(
        id = this[Flights.id].value,
        tripId = this[Flights.tripId],
        shareLink = this[Flights.shareLink],
        source = this[Flights.sourceProvider],
        outbound = json.decodeFromString(this[Flights.outbound]),
        returnSegment = this[Flights.returnSegment]?.let { json.decodeFromString(it) },
        cabinClass = CabinClass.fromString(this[Flights.cabinClass]),
        cabinMixed = this[Flights.cabinMixed],
        notes = this[Flights.notes],
        tags = json.decodeFromString(this[Flights.tags]),
        createdAt = this[Flights.createdAt]
    )
    
    // ===== Prices =====
    
    fun addPrice(price: Price): Long = transaction {
        Prices.insertAndGetId {
            it[flightId] = price.flightId
            it[amount] = price.amount
            it[currency] = price.currency
            it[market] = price.market
            it[checkedAt] = price.checkedAt
        }.value
    }
    
    fun getLatestPrice(flightId: Long): Price? = transaction {
        Prices.selectAll().where { Prices.flightId eq flightId }
            .orderBy(Prices.checkedAt, SortOrder.DESC)
            .firstOrNull()
            ?.toPrice()
    }
    
    fun getPriceHistory(flightId: Long): List<Price> = transaction {
        Prices.selectAll().where { Prices.flightId eq flightId }
            .orderBy(Prices.checkedAt, SortOrder.DESC)
            .map { it.toPrice() }
    }
    
    private fun ResultRow.toPrice() = Price(
        id = this[Prices.id].value,
        flightId = this[Prices.flightId],
        amount = this[Prices.amount],
        currency = this[Prices.currency],
        market = this[Prices.market],
        checkedAt = this[Prices.checkedAt]
    )
    
    // ===== Combined queries =====
    
    fun getFlightsWithPrices(tripId: Long): List<FlightWithPrice> = transaction {
        getFlightsForTrip(tripId).map { flight ->
            FlightWithPrice(
                flight = flight,
                latestPrice = getLatestPrice(flight.id),
                priceHistory = getPriceHistory(flight.id)
            )
        }.sortedBy { it.latestPrice?.amount ?: Double.MAX_VALUE }
    }
}
