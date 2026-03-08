package com.clawfare.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class CabinClass {
    ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST;
    
    companion object {
        fun fromString(s: String): CabinClass = when (s.lowercase()) {
            "economy", "y" -> ECONOMY
            "premium_economy", "premium", "w" -> PREMIUM_ECONOMY
            "business", "j" -> BUSINESS
            "first", "f" -> FIRST
            else -> throw IllegalArgumentException("Unknown cabin class: $s")
        }
    }
    
    override fun toString(): String = name.lowercase()
}

@Serializable
enum class TripType {
    ROUND_TRIP, ONE_WAY
}

@Serializable
data class Trip(
    val id: Long = 0,
    val slug: String,
    val origins: List<String>,
    val destinations: List<String>,
    val departDate: String,
    val returnDate: String?,
    val cabinClass: CabinClass,
    val tripType: TripType = TripType.ROUND_TRIP,
    val maxStops: Int = 1,
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class FlightLeg(
    val flightNumber: String,
    val airline: String,
    val airlineCode: String,
    val departAirport: String,
    val departTime: String,
    val arriveAirport: String,
    val arriveTime: String,
    val durationMinutes: Int,
    val aircraft: String? = null,
    val cabin: CabinClass? = null
)

@Serializable
data class FlightSegment(
    val departAirport: String,
    val departTime: String,
    val arriveAirport: String,
    val arriveTime: String,
    val durationMinutes: Int,
    val stops: Int,
    val legs: List<FlightLeg>
)

@Serializable
data class Flight(
    val id: Long = 0,
    val tripId: Long,
    val shareLink: String,
    val source: String,
    val outbound: FlightSegment,
    val returnSegment: FlightSegment?,
    val cabinClass: CabinClass,
    val cabinMixed: Boolean = false,
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: String = ""
)

@Serializable
data class Price(
    val id: Long = 0,
    val flightId: Long,
    val amount: Double,
    val currency: String,
    val market: String,  // UK, US, EU, JP
    val checkedAt: String
)

/** Summary view for display */
data class FlightWithPrice(
    val flight: Flight,
    val latestPrice: Price?,
    val priceHistory: List<Price> = emptyList()
)
