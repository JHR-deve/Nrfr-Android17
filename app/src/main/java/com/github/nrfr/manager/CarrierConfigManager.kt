package com.github.nrfr.manager

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.github.nrfr.compat.CountryOverrideCoordinator
import com.github.nrfr.model.SimCardInfo

/** Public telephony reads; the country-only write is isolated in the compatibility service. */
object CarrierConfigManager {
    fun getSimCards(context: Context): List<SimCardInfo> {
        val subscriptions = runCatching {
            context.getSystemService(SubscriptionManager::class.java)
                ?.activeSubscriptionInfoList.orEmpty()
        }.getOrDefault(emptyList())
        val telephony = context.getSystemService(TelephonyManager::class.java)
            ?: return emptyList()
        return subscriptions.mapNotNull { info -> runCatching {
            val subId = info.subscriptionId
            if (!SubscriptionManager.isValidSubscriptionId(subId)) return@runCatching null
            val phone = telephony.createForSubscriptionId(subId)
            val country = phone.simCountryIso.orEmpty().lowercase()
            val config = if (country.matches(Regex("[a-z]{2}"))) {
                mapOf("当前国家码" to country.uppercase())
            } else emptyMap()
            SimCardInfo(
                info.simSlotIndex + 1,
                subId,
                info.carrierName?.toString() ?: phone.networkOperatorName.orEmpty(),
                config
            )
        }.getOrNull() }.sortedBy { it.slot }
    }

    fun setCountry(context: Context, subId: Int, countryCode: String): String =
        CountryOverrideCoordinator.apply(context, subId, countryCode)

    fun restoreCountry(context: Context, subId: Int): String =
        CountryOverrideCoordinator.restore(context, subId)
}
