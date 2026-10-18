package com.clawfare

import com.clawfare.cli.*
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) {
    Clawfare()
        .subcommands(
            TripCommand().subcommands(
                TripNew(),
                TripList(),
                TripShow(),
                TripDelete()
            ),
            FlightCommand().subcommands(
                FlightAdd(),
                FlightRefresh(),
                FlightHistory()
            ),
            ServeCommand()
        )
        .main(args)
}
