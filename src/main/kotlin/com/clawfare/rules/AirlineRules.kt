package com.clawfare.rules

/** Airline region classification */
enum class AirlineRegion {
    EUROPEAN, JAPANESE, AMERICAN, KOREAN,
    CHINESE, MIDDLE_EASTERN, AFRICAN, SOUTH_ASIAN, TURKISH
}

/** Airlines grouped by region */
object Airlines {
    val european = setOf(
        "BA", // British Airways
        "LH", // Lufthansa
        "AF", // Air France
        "KL", // KLM
        "LX", // Swiss
        "AY", // Finnair
        "SK", // SAS
        "IB", // Iberia
        "AZ", // ITA Airways
        "TP", // TAP Portugal
        "EI", // Aer Lingus
        "VS", // Virgin Atlantic
        "DY", // Norwegian
        "FI", // Icelandair
        "WK", // Edelweiss
        "SN", // Brussels Airlines
        "LO", // LOT
        "DE", // Condor
        "OS", // Austrian
        "A3", // Aegean
        "EN", // Air Dolomiti
        "2L", // Helvetic
    )
    
    val japanese = setOf(
        "JL", // Japan Airlines
        "NH", // ANA
    )
    
    val american = setOf(
        "AA", // American
        "UA", // United
        "DL", // Delta
        "AC", // Air Canada
        "LA", // LATAM
        "AV", // Avianca
        "AM", // Aeromexico
    )
    
    val korean = setOf(
        "KE", // Korean Air
        "OZ", // Asiana
    )
    
    val chinese = setOf(
        "CA", // Air China
        "MU", // China Eastern
        "CZ", // China Southern
        "HU", // Hainan
        "3U", // Sichuan
    )
    
    val middleEastern = setOf(
        "EK", // Emirates
        "QR", // Qatar
        "EY", // Etihad
        "SV", // Saudia
        "GF", // Gulf Air
        "WY", // Oman Air
    )
    
    val african = setOf(
        "ET", // Ethiopian
        "MS", // EgyptAir
        "SA", // South African
        "KQ", // Kenya Airways
    )
    
    val southAsian = setOf(
        "AI", // Air India
        "6E", // IndiGo
        "UK", // Vistara
        "SG", // SpiceJet
        "MH", // Malaysia Airlines
    )
    
    val turkish = setOf(
        "TK", // Turkish Airlines
        "PC", // Pegasus
    )
    
    fun getRegion(code: String): AirlineRegion? = when (code) {
        in european -> AirlineRegion.EUROPEAN
        in japanese -> AirlineRegion.JAPANESE
        in american -> AirlineRegion.AMERICAN
        in korean -> AirlineRegion.KOREAN
        in chinese -> AirlineRegion.CHINESE
        in middleEastern -> AirlineRegion.MIDDLE_EASTERN
        in african -> AirlineRegion.AFRICAN
        in southAsian -> AirlineRegion.SOUTH_ASIAN
        in turkish -> AirlineRegion.TURKISH
        else -> null
    }
}

/** Filtering rules */
data class FilterRules(
    val allowedRegions: Set<AirlineRegion> = setOf(
        AirlineRegion.EUROPEAN,
        AirlineRegion.JAPANESE,
        AirlineRegion.AMERICAN,
        AirlineRegion.KOREAN
    ),
    val blockedAirlines: Set<String> = emptySet(),
    val allowedAirlines: Set<String> = emptySet(),  // Override: always allow
    val maxStops: Int = 1
) {
    fun isAirlineAllowed(code: String): Boolean {
        // Explicit allow takes precedence
        if (code in allowedAirlines) return true
        // Explicit block
        if (code in blockedAirlines) return false
        // Check region
        val region = Airlines.getRegion(code)
        return region != null && region in allowedRegions
    }
}
