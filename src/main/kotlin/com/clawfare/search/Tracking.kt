package com.clawfare.search

import com.clawfare.db.Database
import com.clawfare.model.CabinClass
import com.clawfare.model.Flight
import com.clawfare.model.FlightLeg
import com.clawfare.model.FlightSegment
import com.clawfare.model.Price
import com.clawfare.model.Trip
import com.clawfare.model.TripType
import java.time.Instant

/** Save a chosen itinerary into the existing trip database the CLI already reads. */
fun trackItinerary(dbPath: String, slug: String, itinerary: Itinerary): String {
    val db = Database(dbPath)
    val clean = slug.trim().lowercase().replace(Regex("[^a-z0-9-]+"), "-").trim('-')
    require(clean.isNotEmpty()) { "Give the trip a short name, like florida-dec." }
    if (db.getTrip(clean) == null) {
        db.createTrip(
            Trip(
                slug = clean,
                origins = listOf(itinerary.origin).filter { it.isNotBlank() },
                destinations = listOf(itinerary.destination).filter { it.isNotBlank() },
                departDate = itinerary.outboundDate,
                returnDate = itinerary.returnDate,
                cabinClass = cabinOf(itinerary.cabinKey),
                tripType = if (itinerary.returnDate == null) TripType.ONE_WAY else TripType.ROUND_TRIP,
                maxStops = maxOf(itinerary.stopsOut, itinerary.stopsRet ?: itinerary.stopsOut),
                notes = itinerary.notes,
            ),
        )
    }
    val trip = db.getTrip(clean) ?: error("Could not store trip $clean")
    val link = itinerary.links.firstOrNull { it.kind == "exact" || it.kind == "partial" }?.url ?: ""
    val flightId = db.addFlight(
        Flight(
            tripId = trip.id,
            shareLink = link,
            source = itinerary.source.ifBlank { "clawfare" },
            outbound = segment(itinerary.outbound, itinerary.origin, itinerary.destination),
            returnSegment = itinerary.returning?.let { segment(it, it.legs.firstOrNull()?.from ?: itinerary.destination, itinerary.origin) },
            cabinClass = cabinOf(itinerary.cabinKey),
            cabinMixed = itinerary.outbound.legs.mapNotNull { it.cabin }.distinct().size > 1,
            notes = itinerary.notes,
            tags = listOf(itinerary.sourceLabel),
        ),
    )
    val amount = itinerary.pricePerPerson ?: 0.0
    db.addPrice(
        Price(
            flightId = flightId,
            amount = amount,
            currency = "GBP",
            market = "UK",
            checkedAt = itinerary.fetchedAt ?: Instant.now().toString(),
        ),
    )
    return clean
}

private fun cabinOf(key: String): CabinClass = when (key) {
    "premium_economy" -> CabinClass.PREMIUM_ECONOMY
    "business" -> CabinClass.BUSINESS
    "first" -> CabinClass.FIRST
    else -> CabinClass.ECONOMY
}

private fun segment(direction: Direction, from: String, to: String): FlightSegment {
    val first = direction.legs.firstOrNull()
    val last = direction.legs.lastOrNull()
    return FlightSegment(
        departAirport = first?.from ?: from,
        departTime = stamp(first?.departDate, first?.departTime),
        arriveAirport = last?.to ?: to,
        arriveTime = stamp(last?.arriveDate, last?.arriveTime),
        durationMinutes = direction.durationMinutes ?: 0,
        stops = direction.stops,
        legs = direction.legs.map { leg ->
            FlightLeg(
                flightNumber = leg.flightNumber,
                airline = leg.airline,
                airlineCode = leg.airlineCode,
                departAirport = leg.from,
                departTime = stamp(leg.departDate, leg.departTime),
                arriveAirport = leg.to,
                arriveTime = stamp(leg.arriveDate, leg.arriveTime),
                durationMinutes = leg.durationMinutes ?: 0,
                aircraft = leg.aircraft,
                cabin = leg.cabin?.let { cabinOf(cabinKeyOf(it)) },
            )
        },
    )
}

private fun stamp(date: String?, time: String?): String =
    if (date.isNullOrBlank()) Instant.now().toString() else date + "T" + (time?.ifBlank { "00:00" } ?: "00:00")
