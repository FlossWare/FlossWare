package org.flossware.civilization.engine

enum class TickType(val years: Double, val description: String) {
    MONTHLY(1.0 / 12.0, "Crisis mode - monthly steps"),
    YEARLY(1.0, "Default - yearly steps"),
    DECADE(10.0, "Low volatility - decade steps");

    companion object {
        fun determineTickType(volatility: Double, stability: Double): TickType {
            return when {
                stability < 0.3 -> MONTHLY
                volatility < 0.1 -> DECADE
                else -> YEARLY
            }
        }
    }
}
