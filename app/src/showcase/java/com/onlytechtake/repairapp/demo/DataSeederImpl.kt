package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.room.withTransaction
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.orders.OrderNumberGenerator
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerActionStatus
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventType
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Showcase implementation of [DataSeeder].
 * Populates the database with a rich set of demo data for quick presentations.
 */
class DataSeederImpl : DataSeeder {
    override suspend fun seed(context: Context) =
        withContext(Dispatchers.IO) {
            ShowcaseDataSeeder.seedShowcaseData(context)
        }

    override val defaultPassword: String = ShowcaseDataSeeder.DEFAULT_PASSWORD
}

object ShowcaseDataSeeder {
    const val DEFAULT_PASSWORD = "ShowcasePassword123"

    suspend fun seedShowcaseData(context: Context) {
        val authPreferences = AuthPreferences(context)
        val mediaStorageProvider = MediaStorageProvider(context)
        val orderNumberGenerator = OrderNumberGenerator()

        // We don't pre-fill owner info here anymore to allow the Showcase user
        // to see the initial Setup Wizard (pre-filled in the UI).

        val db = RepairAppDatabase.getInstance(context)
        val now = System.currentTimeMillis()
        val yearPrefix = orderNumberGenerator.yearPrefix(now)
        val passwordHasher = PasswordHasher()
        val pwdHash = passwordHasher.hash(DEFAULT_PASSWORD.toCharArray())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        db.withTransaction {
            // 1. Create Operators
            val operators =
                listOf(
                    UserEntity(
                        id = "op-reception",
                        displayName = context.getString(R.string.showcase_op_reception),
                        username = "anna.reception",
                        passwordHash = pwdHash.hash,
                        passwordSalt = pwdHash.salt,
                        role = UserRole.OPERATOR,
                        permissions =
                            setOf(
                                UserPermission.CREATE_ORDERS,
                                UserPermission.EDIT_OWN_ORDERS,
                                UserPermission.MANAGE_CUSTOMERS,
                                UserPermission.MANAGE_PAYMENTS,
                                UserPermission.RECORD_VIDEO,
                            ),
                        active = true,
                        forcePasswordChange = false,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    UserEntity(
                        id = "op-technician",
                        displayName = context.getString(R.string.showcase_op_technician),
                        username = "marco.tech",
                        passwordHash = pwdHash.hash,
                        passwordSalt = pwdHash.salt,
                        role = UserRole.OPERATOR,
                        permissions =
                            setOf(
                                UserPermission.EDIT_ALL_ORDERS,
                                UserPermission.MANAGE_CATEGORIES,
                                UserPermission.CLOSE_ORDERS,
                                UserPermission.RECORD_VIDEO,
                            ),
                        active = true,
                        forcePasswordChange = false,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    UserEntity(
                        id = "op-manager",
                        displayName = context.getString(R.string.showcase_op_manager),
                        username = "elena.manager",
                        passwordHash = pwdHash.hash,
                        passwordSalt = pwdHash.salt,
                        role = UserRole.OPERATOR,
                        permissions =
                            setOf(
                                UserPermission.EXPORT_DATA,
                                UserPermission.MANAGE_USERS,
                                UserPermission.MANAGE_TEMPLATES,
                                UserPermission.BACKUP_RESTORE,
                            ),
                        active = true,
                        forcePasswordChange = false,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    UserEntity(
                        id = "admin-showcase",
                        displayName = context.getString(R.string.showcase_admin),
                        username = "admin",
                        passwordHash = pwdHash.hash,
                        passwordSalt = pwdHash.salt,
                        role = UserRole.ADMIN,
                        permissions = UserPermission.entries.toSet(),
                        active = true,
                        forcePasswordChange = false,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            db.userDao().upsertAll(operators)

            // 2. Create Categories
            val categories =
                listOf(
                    OrderCategoryEntity(
                        "cat-smartphone",
                        null,
                        0,
                        context.getString(R.string.common_cat_smartphone),
                        context.getString(R.string.common_cat_smartphone_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-apple",
                        "cat-smartphone",
                        1,
                        context.getString(R.string.common_cat_apple),
                        context.getString(R.string.showcase_cat_apple_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-iphone",
                        "cat-apple",
                        2,
                        context.getString(R.string.common_cat_iphone),
                        context.getString(R.string.showcase_cat_iphone_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-iphone-14-pro",
                        "cat-iphone",
                        3,
                        context.getString(R.string.common_cat_iphone_14_pro),
                        context.getString(R.string.showcase_cat_iphone_14_pro_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-iphone-13",
                        "cat-iphone",
                        3,
                        context.getString(R.string.common_cat_iphone_13),
                        context.getString(R.string.showcase_cat_iphone_13_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-samsung",
                        "cat-smartphone",
                        1,
                        context.getString(R.string.common_cat_samsung),
                        context.getString(R.string.showcase_cat_samsung_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-galaxy-s",
                        "cat-samsung",
                        2,
                        context.getString(R.string.common_cat_galaxy_s),
                        context.getString(R.string.showcase_cat_galaxy_s_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-s23-ultra",
                        "cat-galaxy-s",
                        3,
                        context.getString(R.string.common_cat_s23_ultra),
                        context.getString(R.string.showcase_cat_s23_ultra_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-pc",
                        null,
                        0,
                        context.getString(R.string.common_cat_pc),
                        context.getString(R.string.common_cat_pc_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-notebook",
                        "cat-pc",
                        1,
                        context.getString(R.string.common_cat_notebook),
                        context.getString(R.string.common_cat_notebook_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-gaming-laptop",
                        "cat-notebook",
                        2,
                        context.getString(R.string.common_cat_gaming),
                        context.getString(R.string.common_cat_gaming_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-rog-strix",
                        "cat-gaming-laptop",
                        3,
                        context.getString(R.string.common_cat_rog_strix),
                        context.getString(R.string.common_cat_rog_strix_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-windows-laptop",
                        "cat-notebook",
                        2,
                        context.getString(R.string.common_cat_windows_laptop),
                        context.getString(R.string.showcase_cat_windows_laptop_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-asus-zenbook",
                        "cat-windows-laptop",
                        3,
                        context.getString(R.string.common_cat_asus_zenbook),
                        context.getString(R.string.showcase_cat_asus_zenbook_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-console",
                        null,
                        0,
                        context.getString(R.string.showcase_cat_console),
                        context.getString(R.string.showcase_cat_console_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-sony",
                        "cat-console",
                        1,
                        context.getString(R.string.common_cat_sony),
                        context.getString(R.string.showcase_cat_sony_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-ps5",
                        "cat-sony",
                        2,
                        context.getString(R.string.common_cat_ps5),
                        context.getString(R.string.showcase_cat_ps5_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-ps5-hdmi",
                        "cat-ps5",
                        3,
                        context.getString(R.string.showcase_cat_ps5_hdmi),
                        context.getString(R.string.showcase_cat_ps5_hdmi_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-dualsense",
                        "cat-ps5",
                        3,
                        context.getString(R.string.common_cat_dualsense),
                        context.getString(R.string.showcase_cat_dualsense_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-nintendo",
                        "cat-console",
                        1,
                        context.getString(R.string.common_cat_nintendo),
                        context.getString(R.string.common_cat_nintendo_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-switch",
                        "cat-nintendo",
                        2,
                        context.getString(R.string.common_cat_switch),
                        context.getString(R.string.common_cat_switch_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-joycon",
                        "cat-switch",
                        3,
                        context.getString(R.string.common_cat_joycon),
                        context.getString(R.string.demo_cat_analog_service),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-tablet",
                        null,
                        0,
                        context.getString(R.string.common_cat_tablet),
                        context.getString(R.string.showcase_cat_tablet_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-ipad",
                        "cat-tablet",
                        1,
                        context.getString(R.string.common_cat_ipad),
                        context.getString(R.string.showcase_cat_ipad_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-ipad-pro",
                        "cat-ipad",
                        2,
                        context.getString(R.string.common_cat_ipad_pro),
                        context.getString(R.string.showcase_cat_ipad_pro_desc),
                        active = true,
                    ),
                    OrderCategoryEntity(
                        "cat-ipad-glass",
                        "cat-ipad-pro",
                        3,
                        context.getString(R.string.showcase_cat_ipad_glass),
                        context.getString(R.string.showcase_cat_ipad_glass_desc),
                        active = true,
                    ),
                )
            db.orderCategoryDao().upsertAll(categories)

            // 3. Create Private Customers
            val privateNames =
                listOf(
                    R.string.showcase_private_name_1,
                    R.string.showcase_private_name_2,
                    R.string.showcase_private_name_3,
                    R.string.showcase_private_name_4,
                    R.string.showcase_private_name_5,
                )
            val privateSurnames =
                listOf(
                    R.string.showcase_private_surname_1,
                    R.string.showcase_private_surname_2,
                    R.string.showcase_private_surname_3,
                    R.string.showcase_private_surname_4,
                    R.string.showcase_private_surname_5,
                )

            val privateCustomers =
                (1..6).map { i ->
                    val name =
                        if (i <= 5) {
                            context.getString(privateNames[i - 1])
                        } else {
                            context.getString(R.string.showcase_private_fictitious_name)
                        }
                    val surname =
                        if (i <= 5) {
                            context.getString(privateSurnames[i - 1])
                        } else {
                            context.getString(R.string.showcase_private_fictitious_surname)
                        }

                    CustomerEntity(
                        id = "cust-p-$i",
                        type = CustomerType.PRIVATE,
                        firstName = name,
                        lastName = surname,
                        companyName = null,
                        vatNumber = null,
                        taxCode =
                            if (i <= 5) {
                                context.getString(
                                    R.string.showcase_tax_code_prefix,
                                ) + i + UUID.randomUUID().toString().take(5)
                            } else {
                                context.getString(R.string.showcase_fictitious_tax_code)
                            },
                        phone = context.getString(R.string.showcase_phone_prefix) + i,
                        email = context.getString(R.string.showcase_private_email_format, i),
                        address = context.getString(R.string.showcase_private_address, i),
                        notes = context.getString(R.string.showcase_customer_notes),
                        createdAt = now - (i * 86400000L),
                        updatedAt = now - (i * 86400000L),
                    )
                }
            db.customerDao().upsertAll(privateCustomers)

            // 4. Create Business Customers
            val businessNameIds =
                listOf(
                    R.string.showcase_business_name_1,
                    R.string.showcase_business_name_2,
                    R.string.showcase_business_name_3,
                    R.string.showcase_business_name_4,
                    R.string.showcase_business_name_5,
                )
            val businessCustomers =
                (1..6).map { i ->
                    val businessName =
                        if (i <= 5) {
                            context.getString(businessNameIds[i - 1])
                        } else {
                            context.getString(R.string.showcase_business_fictitious)
                        }

                    CustomerEntity(
                        id = "cust-b-$i",
                        type = CustomerType.BUSINESS,
                        firstName = null,
                        lastName = null,
                        companyName = businessName,
                        vatNumber =
                            if (i <= 5) {
                                context.getString(
                                    R.string.showcase_vat_format,
                                    i,
                                )
                            } else {
                                context.getString(R.string.showcase_vat_fictitious)
                            },
                        taxCode =
                            if (i <= 5) {
                                context.getString(
                                    R.string.showcase_tax_code_prefix,
                                ) + context.getString(R.string.showcase_tax_code_business_prefix) + i +
                                    UUID
                                        .randomUUID()
                                        .toString()
                                        .take(5)
                            } else {
                                context.getString(R.string.showcase_fictitious_business_tax_code)
                            },
                        phone = context.getString(R.string.showcase_phone_prefix) + (i + 5),
                        email = context.getString(R.string.showcase_business_email_format, i),
                        address = context.getString(R.string.showcase_business_address, i),
                        notes = context.getString(R.string.showcase_customer_notes),
                        createdAt = now - (i * 86400000L),
                        updatedAt = now - (i * 86400000L),
                    )
                }
            db.customerDao().upsertAll(businessCustomers)

            // 5. Create Orders
            val allCustomers = privateCustomers + businessCustomers
            val statuses = OrderStatus.entries.toTypedArray()
            val selectableCategories =
                categories.filter { cat -> categories.none { it.parentId == cat.id } }

            val repairTypes =
                listOf(
                    context.getString(R.string.showcase_type_pc),
                    context.getString(R.string.showcase_type_smartphone),
                    context.getString(R.string.showcase_type_tablet),
                    context.getString(R.string.showcase_type_console),
                )

            var orderCount = 1
            allCustomers.forEach { customer ->
                repeat(3) { j ->
                    val orderId = "order-$orderCount"
                    val isClosed = (j == 2)
                    val status =
                        if (isClosed) {
                            OrderStatus.CLOSED
                        } else if (j == 1) {
                            OrderStatus.READY_FOR_PICKUP
                        } else {
                            statuses[(orderCount % (statuses.size - 2))]
                        }

                    val categoryId =
                        selectableCategories[(orderCount % selectableCategories.size)].id
                    val operatorId = operators[(orderCount % 3)].id
                    val repairType = repairTypes[orderCount % repairTypes.size]
                    val priority =
                        if ((orderCount % 5) == 0) OrderPriority.URGENT else OrderPriority.NORMAL

                    val order =
                        RepairOrderEntity(
                            id = orderId,
                            orderNumber = yearPrefix + orderCount.toString().padStart(6, '0'),
                            customerId = customer.id,
                            title =
                                context.getString(
                                    R.string.showcase_order_title,
                                    repairType,
                                    j + 1,
                                ),
                            categoryId = categoryId,
                            status = status,
                            priority = priority,
                            assignedToUserId = operatorId,
                            createdByUserId = "op-reception",
                            description = context.getString(R.string.showcase_order_description),
                            internalNotes = context.getString(R.string.showcase_order_internal_notes),
                            estimateAmountCents = if (status != OrderStatus.OPEN) 5000L else null,
                            finalAmountCents = if (isClosed) 4500L else null,
                            diagnosisOutcome = if (status != OrderStatus.OPEN) DiagnosisOutcome.REPAIRABLE else null,
                            createdAt = now - (orderCount * 3600000L * 2),
                            updatedAt = now - (orderCount * 3600000L),
                            closedAt = if (isClosed) now - (orderCount * 1800000L) else null,
                            closureReason = if (isClosed) ClosureReason.COMPLETED else null,
                            customerActionStatus = if (status == OrderStatus.RESULT) CustomerActionStatus.WAITING_ACCEPTANCE else CustomerActionStatus.NONE,
                            closeNote = if (isClosed) context.getString(R.string.showcase_order_closed_note) else null,
                            closedByUserId = if (isClosed) operatorId else null,
                            categoryPathSnapshot = context.getString(R.string.showcase_cat_snapshot_demo),
                            lastCustomerContactAt = now - (orderCount * 1000000L),
                        )
                    db.repairOrderDao().upsert(order)

                    // 1. Intake Event
                    db.timelineEventDao().upsert(
                        TimelineEventEntity(
                            id = "event-$orderId-init",
                            orderId = orderId,
                            type = TimelineEventType.ORDER_CREATED,
                            title = context.getString(R.string.order_created_title),
                            description = context.getString(R.string.showcase_event_intake_desc),
                            oldStatus = null,
                            newStatus = OrderStatus.OPEN.name,
                            visibleToCustomer = true,
                            requiresCustomerFeedback = false,
                            expectedFeedbackType = null,
                            customerFeedback = null,
                            customerFeedbackNote = null,
                            customerFeedbackAt = null,
                            createdByUserId = "op-reception",
                            createdAt = order.createdAt,
                        ),
                    )

                    // 2. Diagnosis Event (if not OPEN)
                    if (status != OrderStatus.OPEN) {
                        db.timelineEventDao().upsert(
                            TimelineEventEntity(
                                id = "event-$orderId-diag",
                                orderId = orderId,
                                type = TimelineEventType.STATUS_CHANGED,
                                title = context.getString(R.string.status_diagnosis),
                                description = context.getString(R.string.showcase_event_diagnosis_desc),
                                oldStatus = OrderStatus.OPEN.name,
                                newStatus = OrderStatus.DIAGNOSIS.name,
                                visibleToCustomer = true,
                                requiresCustomerFeedback = true,
                                expectedFeedbackType = CustomerFeedbackType.ACCEPT,
                                customerFeedback = CustomerFeedbackType.ACCEPT,
                                customerFeedbackNote = context.getString(R.string.showcase_feedback_ok),
                                customerFeedbackAt = order.createdAt + 1800000L,
                                createdByUserId = operatorId,
                                createdAt = order.createdAt + 1700000L,
                            ),
                        )
                    }

                    // 3. Ready Event (if READY or CLOSED)
                    if ((status == OrderStatus.READY_FOR_PICKUP) || (status == OrderStatus.CLOSED)) {
                        db.timelineEventDao().upsert(
                            TimelineEventEntity(
                                id = "event-$orderId-ready",
                                orderId = orderId,
                                type = TimelineEventType.STATUS_CHANGED,
                                title = context.getString(R.string.status_ready_for_pickup),
                                description = context.getString(R.string.showcase_event_ready_desc),
                                oldStatus = OrderStatus.IN_PROGRESS.name,
                                newStatus = OrderStatus.READY_FOR_PICKUP.name,
                                visibleToCustomer = true,
                                requiresCustomerFeedback = false,
                                expectedFeedbackType = null,
                                customerFeedback = null,
                                customerFeedbackNote = null,
                                customerFeedbackAt = null,
                                createdByUserId = operatorId,
                                createdAt = order.updatedAt - 900000L,
                            ),
                        )
                    }

                    if (isClosed) {
                        db.timelineEventDao().upsert(
                            TimelineEventEntity(
                                id = "event-$orderId-closed",
                                orderId = orderId,
                                type = TimelineEventType.STATUS_CHANGED,
                                title = context.getString(R.string.status_closed),
                                description = context.getString(R.string.showcase_event_closed_desc),
                                oldStatus = OrderStatus.READY_FOR_PICKUP.name,
                                newStatus = OrderStatus.CLOSED.name,
                                visibleToCustomer = true,
                                requiresCustomerFeedback = false,
                                expectedFeedbackType = null,
                                customerFeedback = null,
                                customerFeedbackNote = null,
                                customerFeedbackAt = null,
                                createdByUserId = operatorId,
                                createdAt = order.closedAt!!,
                            ),
                        )
                    }

                    val photoFile =
                        writeDemoBitmap(
                            context = context,
                            directory = mediaStorageProvider.getOrderMediaDir(orderId),
                            fileName = "photo-$orderId.png",
                            label = context.getString(R.string.showcase_photo_label, order.title),
                            backgroundColor = Color.LTGRAY,
                            paint = paint,
                            width = 1280,
                            height = 720,
                        )
                    db.orderMediaDao().upsert(
                        OrderMediaEntity(
                            id = "media-$orderId",
                            orderId = orderId,
                            mediaType = context.getString(R.string.showcase_media_type_photo),
                            localPath = photoFile.absolutePath,
                            fileName = photoFile.name,
                            mimeType = context.getString(R.string.showcase_mime_type_png),
                            sizeBytes = photoFile.length(),
                            width = 1280,
                            height = 720,
                            createdByUserId = operatorId,
                            capturedAt = order.createdAt + 1000,
                            createdAt = order.createdAt + 1000,
                        ),
                    )

                    val sigFile =
                        writeDemoBitmap(
                            context = context,
                            directory = mediaStorageProvider.getOrderSignaturesDir(orderId),
                            fileName = "sig-$orderId.png",
                            label =
                                context.getString(
                                    R.string.showcase_sig_label,
                                    customer.firstName ?: customer.companyName,
                                ),
                            backgroundColor = Color.WHITE,
                            paint = paint,
                            width = 800,
                            height = 400,
                        )
                    db.signatureDao().upsert(
                        SignatureEntity(
                            id = "sig-$orderId",
                            orderId = orderId,
                            signerName =
                                customer.firstName ?: customer.companyName
                                ?: context.getString(R.string.signature_default_signer),
                            privacyAccepted = true,
                            termsAccepted = true,
                            privacyTextSnapshot = context.getString(R.string.showcase_privacy_snapshot),
                            privacyTemplateVersion = 1,
                            termsTextSnapshot = context.getString(R.string.showcase_terms_snapshot),
                            termsTemplateVersion = 1,
                            localPath = sigFile.absolutePath,
                            fileName = sigFile.name,
                            mimeType = context.getString(R.string.showcase_mime_type_png),
                            sizeBytes = sigFile.length(),
                            width = 800,
                            height = 400,
                            createdByUserId = "op-reception",
                            signedAt = order.createdAt + 2000,
                            createdAt = order.createdAt + 2000,
                        ),
                    )

                    orderCount++
                }
            }

            // 6. Create Templates
            val templateConfigs =
                listOf(
                    TemplateConfig(
                        "privacy_breve",
                        R.string.template_privacy_breve_title,
                        R.string.template_privacy_breve_body,
                        R.string.anchor_step_legal,
                    ),
                    TemplateConfig(
                        "condizioni_presa_in_carico",
                        R.string.template_condizioni_title,
                        R.string.template_condizioni_body,
                        R.string.anchor_step_legal,
                    ),
                    TemplateConfig(
                        "whatsapp_presa_in_carico",
                        R.string.template_wa_intake_title,
                        R.string.template_wa_intake_body,
                        R.string.anchor_step_intake,
                    ),
                    TemplateConfig(
                        "whatsapp_aggiornamento",
                        R.string.template_wa_update_title,
                        R.string.template_wa_update_body,
                        R.string.anchor_step_update,
                    ),
                    TemplateConfig(
                        "whatsapp_diagnosi",
                        R.string.template_wa_diagnosis_title,
                        R.string.template_wa_diagnosis_body,
                        R.string.anchor_step_diagnosis,
                    ),
                    TemplateConfig(
                        "whatsapp_preventivo_intervento",
                        R.string.template_wa_quote_title,
                        R.string.template_wa_quote_body,
                        R.string.anchor_step_quote,
                    ),
                    TemplateConfig(
                        "whatsapp_non_riparabile",
                        R.string.template_wa_not_repairable_title,
                        R.string.template_wa_not_repairable_body,
                        R.string.anchor_step_outcome,
                    ),
                    TemplateConfig(
                        "whatsapp_riparazione",
                        R.string.template_wa_repairing_title,
                        R.string.template_wa_repairing_body,
                        R.string.anchor_step_repair,
                    ),
                    TemplateConfig(
                        "whatsapp_completamento",
                        R.string.template_wa_completamento_title,
                        R.string.template_wa_completamento_body,
                        R.string.anchor_step_closed,
                    ),
                    TemplateConfig(
                        "whatsapp_pronto_ritiro",
                        R.string.template_wa_ready_title,
                        R.string.template_wa_ready_body,
                        R.string.anchor_step_pickup,
                    ),
                    TemplateConfig(
                        "whatsapp_stato_avanzamento_pdf",
                        R.string.template_wa_pdf_status_title,
                        R.string.template_wa_pdf_status_body,
                        R.string.anchor_step_docs,
                    ),
                )

            val templates =
                templateConfigs.mapIndexed { index, config ->
                    MessageTemplateEntity(
                        id = "template-$index",
                        code = config.code,
                        title = context.getString(config.titleRes),
                        channel = MessageChannel.WHATSAPP,
                        body = context.getString(config.bodyRes),
                        active = true,
                        version = 1,
                        updatedByUserId = "admin-showcase",
                        updatedAt = now,
                        step = context.getString(config.stepRes),
                    )
                }
            db.messageTemplateDao().upsertAll(templates)

            // Reset Anchor Mappings to defaults to avoid broken links from previous sessions
            db.appSettingDao().deleteByKeys(
                com.onlytechtake.repairapp.core.templates.TemplateAnchor.entries
                    .map { it.settingKey },
            )
        }

        authPreferences.markConfigured()
    }

    private data class TemplateConfig(
        val code: String,
        val titleRes: Int,
        val bodyRes: Int,
        val stepRes: Int,
    )

    private fun writeDemoBitmap(
        context: Context,
        directory: File,
        fileName: String,
        label: String,
        backgroundColor: Int,
        paint: Paint,
        width: Int,
        height: Int,
    ): File {
        directory.mkdirs()
        val file = File(directory, fileName)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)
        paint.color = Color.BLACK
        paint.textSize = 40f
        canvas.drawText(label, 50f, 100f, paint)
        paint.textSize = 24f
        canvas.drawText(context.getString(R.string.showcase_bitmap_footer), 50f, 150f, paint)

        paint.strokeWidth = 5f
        paint.color = Color.BLUE
        canvas.drawLine(100f, 300f, 1100f, 600f, paint)
        canvas.drawLine(100f, 600f, 1100f, 300f, paint)

        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)
        }
        bitmap.recycle()
        return file
    }
}
