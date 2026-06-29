package com.onlytechtake.repairapp.core.demo

import android.content.Context
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo

@Suppress("SameReturnValue", "ConstPropertyName")
object DemoMode {
    var isEnabled: Boolean = false
        private set

    fun canEnable(): Boolean = BuildConfig.IS_DEMO_MODE_ENABLED

    fun setEnabled(enabled: Boolean) {
        if (canEnable()) {
            isEnabled = enabled
        }
    }

    fun getVersionBanner(context: Context): String = context.getString(R.string.demo_version_banner)

    fun getAdminDisplayName(context: Context): String = context.getString(R.string.demo_admin_display_name)

    fun getOperatorNotAvailableMessage(context: Context): String = context.getString(R.string.demo_error_operator_not_available)

    fun getOperatorLimitMessage(
        context: Context,
        limit: Int,
    ): String = context.resources.getQuantityString(R.plurals.demo_error_operator_limit, limit, limit)

    fun getOrderLimitMessage(
        context: Context,
        limit: Int,
    ): String = context.resources.getQuantityString(R.plurals.demo_error_order_limit, limit, limit)

    fun getShowcaseInitializingMessage(context: Context): String = ""

    fun getShowcasePreparingMessage(context: Context): String = ""

    fun getDemoActionLabel(context: Context): String = context.getString(R.string.auth_demo_action)

    fun getShowcaseScenarioLabel(context: Context): String = ""

    fun getShowcaseWalkthroughLabel(context: Context): String = ""

    fun getDemoTryCta(context: Context): String = context.getString(R.string.home_demo_cta_try)

    fun getDemoProFeaturesLabel(context: Context): String = context.getString(R.string.home_demo_pro_features)

    fun getDemoProFeaturesDesc(context: Context): String = context.getString(R.string.home_demo_pro_features_description)

    fun getShowcaseOwnerInfo(context: Context): OwnerInfo? = null

    fun getDemoCustomer(context: Context) =
        CustomerEntity(
            id = "demo-customer-1",
            type = CustomerType.PRIVATE,
            firstName = context.getString(R.string.demo_customer_first_name),
            lastName = context.getString(R.string.demo_customer_last_name),
            companyName = null,
            vatNumber = null,
            taxCode = null,
            phone = context.getString(R.string.demo_customer_phone),
            email = context.getString(R.string.demo_customer_email),
            address = context.getString(R.string.demo_customer_address),
            notes = context.getString(R.string.demo_customer_notes),
            createdAt = 1735689600000L, // 2025-01-01
            updatedAt = 1735689600000L,
        )

    fun getDemoCategories(context: Context) =
        listOf(
            OrderCategoryEntity(
                id = "cat-smartphone",
                parentId = null,
                level = 0,
                name = context.getString(R.string.common_cat_smartphone),
                description = context.getString(R.string.common_cat_smartphone_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-apple",
                parentId = "cat-smartphone",
                level = 1,
                name = context.getString(R.string.common_cat_apple),
                description = context.getString(R.string.common_cat_apple_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-iphone",
                parentId = "cat-apple",
                level = 2,
                name = context.getString(R.string.common_cat_iphone),
                description = context.getString(R.string.common_cat_iphone_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-iphone-13",
                parentId = "cat-iphone",
                level = 3,
                name = "iPhone 13",
                description = "Display Service",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-samsung",
                parentId = "cat-smartphone",
                level = 1,
                name = context.getString(R.string.common_cat_samsung),
                description = context.getString(R.string.common_cat_samsung_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-galaxy-s",
                parentId = "cat-samsung",
                level = 2,
                name = context.getString(R.string.common_cat_galaxy_s),
                description = context.getString(R.string.common_cat_galaxy_s_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-s23-ultra",
                parentId = "cat-galaxy-s",
                level = 3,
                name = "S23 Ultra",
                description = "Glass Service",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-console",
                parentId = null,
                level = 0,
                name = context.getString(R.string.common_cat_console),
                description = context.getString(R.string.common_cat_console_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-sony",
                parentId = "cat-console",
                level = 1,
                name = context.getString(R.string.common_cat_sony),
                description = context.getString(R.string.common_cat_sony_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-ps5",
                parentId = "cat-sony",
                level = 2,
                name = context.getString(R.string.common_cat_ps5),
                description = context.getString(R.string.common_cat_ps5_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-dualsense",
                parentId = "cat-ps5",
                level = 3,
                name = "DualSense",
                description = "Drift Service",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-nintendo",
                parentId = "cat-console",
                level = 1,
                name = context.getString(R.string.common_cat_nintendo),
                description = context.getString(R.string.common_cat_nintendo_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-switch",
                parentId = "cat-nintendo",
                level = 2,
                name = context.getString(R.string.common_cat_switch),
                description = context.getString(R.string.common_cat_switch_desc),
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-joycon",
                parentId = "cat-switch",
                level = 3,
                name = "Joy-Con",
                description = "Analog replacement",
                active = true,
            ),
        )
}
