package com.github.nrfr.model

data class SimCardInfo(
    val slot: Int,
    val subId: Int,
    val carrierName: String,
    val countryIso: String,
    val operatorNumeric: String,
    val networkCountryIso: String,
    val isDefaultData: Boolean,
    val isReady: Boolean
)
