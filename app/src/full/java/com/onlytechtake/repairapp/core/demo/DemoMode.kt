package com.onlytechtake.repairapp.core.demo

import android.content.Context
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo

@Suppress("SameReturnValue", "ConstPropertyName")
object DemoMode {
    const val isEnabled: Boolean = false
    var disableAutoSeed: Boolean = false

    fun canEnable(): Boolean = false

    fun setEnabled(
        enabled: Boolean,
    ) {
        // No-op in full version
    }

    fun getVersionBanner(context: Context): String = ""

    fun getAdminDisplayName(context: Context): String = ""

    fun getOperatorNotAvailableMessage(context: Context): String = ""

    fun getOperatorLimitMessage(
        context: Context,
        limit: Int,
    ): String = ""

    fun getOrderLimitMessage(
        context: Context,
        limit: Int,
    ): String = ""

    fun getShowcaseInitializingMessage(context: Context): String = ""

    fun getShowcasePreparingMessage(context: Context): String = ""

    fun getDemoActionLabel(context: Context): String = ""

    fun getShowcaseScenarioLabel(context: Context): String = ""

    fun getShowcaseWalkthroughLabel(context: Context): String = ""

    fun getDemoTryCta(context: Context): String = ""

    fun getDemoProFeaturesLabel(context: Context): String = ""

    fun getDemoProFeaturesDesc(context: Context): String = ""

    fun getShowcaseOwnerInfo(context: Context): OwnerInfo? = null

    fun getDemoCustomer(context: Context): CustomerEntity =
        throw IllegalStateException("DemoMode not available")

    fun getDemoCategories(context: Context): List<OrderCategoryEntity> =
        throw IllegalStateException("DemoMode not available")
}
