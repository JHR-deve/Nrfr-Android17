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
        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        return subscriptions.mapNotNull { info -> runCatching {
            val subId = info.subscriptionId
            if (!SubscriptionManager.isValidSubscriptionId(subId) || info.simSlotIndex < 0) {
                return@runCatching null
            }
            val phone = telephony.createForSubscriptionId(subId)
            SimCardInfo(
                info.simSlotIndex + 1,
                subId,
                info.carrierName?.toString() ?: phone.networkOperatorName.orEmpty(),
                phone.simCountryIso.orEmpty().uppercase(),
                phone.simOperator.orEmpty(),
                phone.networkCountryIso.orEmpty().uppercase(),
                subId == defaultDataSubId,
                phone.simState == TelephonyManager.SIM_STATE_READY
            )
        }.getOrNull() }.sortedBy { it.slot }
    }

    fun setCountry(context: Context, subId: Int, countryCode: String): String =
        CountryOverrideCoordinator.apply(context, subId, countryCode)

    fun restoreCountry(context: Context, subId: Int): String =
        CountryOverrideCoordinator.restore(context, subId)
}
