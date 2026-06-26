package com.onlytechtake.repairapp.core.demo

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo

@Suppress("SameReturnValue", "ConstPropertyName")
object DemoMode {
    const val isEnabled: Boolean = false

    fun canEnable(): Boolean = false

    fun setEnabled(
        enabled: Boolean,
    ) {
        // No-op in showcase version
    }

    fun getVersionBanner(context: Context): String = context.getString(R.string.demo_version_banner)

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

    fun getShowcaseInitializingMessage(context: Context): String = context.getString(R.string.auth_showcase_initializing)

    fun getShowcasePreparingMessage(context: Context): String = context.getString(R.string.auth_showcase_preparing)

    fun getDemoActionLabel(context: Context): String = ""

    fun getShowcaseScenarioLabel(context: Context): String = context.getString(R.string.home_showcase_scenario)

    fun getShowcaseWalkthroughLabel(context: Context): String = context.getString(R.string.home_showcase_walkthrough)

    fun getDemoTryCta(context: Context): String = ""

    fun getDemoProFeaturesLabel(context: Context): String = ""

    fun getDemoProFeaturesDesc(context: Context): String = ""

    fun getShowcaseOwnerInfo(context: Context): OwnerInfo =
        OwnerInfo(
            name = context.getString(R.string.showcase_owner_name),
            vatNumber = context.getString(R.string.showcase_owner_vat),
            address = context.getString(R.string.showcase_owner_address),
            email = context.getString(R.string.showcase_owner_email),
            phone = context.getString(R.string.showcase_owner_phone),
        )

    val demoCustomer: CustomerEntity get() = throw IllegalStateException("DemoMode non disponibile")
    val demoCategories: List<OrderCategoryEntity> get() = throw IllegalStateException("DemoMode non disponibile")
}
