package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
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
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo
import java.io.File
import java.io.FileOutputStream
import com.onlytechtake.repairapp.test.R as TestR

object DemoDataSeeder {
    const val DEMO_PASSWORD = "DemoPass123"
    const val DEMO_ADMIN_USERNAME = "admin.demo"
    const val DEMO_OPERATOR_USERNAME = "operator.demo"
    private const val DEMO_ADMIN_ID = "demo-admin"
    private const val DEMO_OPERATOR_ID = "demo-operator"
    const val DEMO_CATEGORY_ID = "demo-category-notebook"
    private const val DEMO_CUSTOMER_ID = "demo-customer"
    const val DEMO_ORDER_ID = "demo-order-operator-intake"

    private val testContext: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    suspend fun seedDemoProfile(
        context: Context,
        locale: java.util.Locale = java.util.Locale.ENGLISH,
    ) {
        RepairAppDatabase.closeInstance()
        context.deleteDatabase(RepairAppDatabase.DATABASE_NAME)
        AuthPreferences(context).clear()
        val ownerPreferences = OwnerPreferences(context)
        ownerPreferences.updateOwnerInfo(
            OwnerInfo(
                name = "OnlyTechTake HQ",
                vatNumber = "US987654321",
                address = "123 Demo Street, New York",
                email = "demo@onlytechtake.invalid",
                phone = "+1 555-987-6543",
            ),
        )

        val mediaStorageProvider = MediaStorageProvider(context)
        mediaStorageProvider.getBaseMediaDir().deleteRecursively()
        val orderNumberGenerator =
            com.onlytechtake.repairapp.core.orders
                .OrderNumberGenerator()

        val now = 1_780_830_000_000L
        val yearPrefix = orderNumberGenerator.yearPrefix(now)
        val passwordHasher = PasswordHasher()
        val adminHash = passwordHasher.hash(DEMO_PASSWORD.toCharArray())
        val operatorHash = passwordHasher.hash(DEMO_PASSWORD.toCharArray())
        val database = RepairAppDatabase.getInstance(context)

        database.userDao().upsert(
            UserEntity(
                id = DEMO_ADMIN_ID,
                displayName = testContext.getString(TestR.string.demo_admin_display_name),
                username = DEMO_ADMIN_USERNAME,
                passwordHash = adminHash.hash,
                passwordSalt = adminHash.salt,
                role = UserRole.ADMIN,
                active = true,
                createdAt = now,
                updatedAt = now,
            ),
        )

        // Only seed operator if NOT in demo version
        if (!BuildConfig.IS_DEMO_VERSION) {
            database.userDao().upsert(
                UserEntity(
                    id = DEMO_OPERATOR_ID,
                    displayName = testContext.getString(TestR.string.demo_operator_display_name),
                    username = DEMO_OPERATOR_USERNAME,
                    passwordHash = operatorHash.hash,
                    passwordSalt = operatorHash.salt,
                    role = UserRole.OPERATOR,
                    permissions =
                        setOf(
                            UserPermission.CREATE_ORDERS,
                            UserPermission.EDIT_OWN_ORDERS,
                            UserPermission.CLOSE_ORDERS,
                            UserPermission.MANAGE_CUSTOMERS,
                            UserPermission.MANAGE_PAYMENTS,
                            UserPermission.RECORD_VIDEO,
                        ),
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        database.orderCategoryDao().upsertAll(
            listOf(
                OrderCategoryEntity(
                    id = "demo-cat-pc",
                    parentId = null,
                    level = 0,
                    name = context.getString(R.string.common_cat_pc),
                    description = context.getString(R.string.common_cat_pc_desc),
                    active = true,
                ),
                OrderCategoryEntity(
                    id = DEMO_CATEGORY_ID, // notebook
                    parentId = "demo-cat-pc",
                    level = 1,
                    name = context.getString(R.string.common_cat_notebook),
                    description = context.getString(R.string.common_cat_notebook_desc),
                    active = true,
                ),
                OrderCategoryEntity(
                    id = "demo-cat-gaming",
                    parentId = DEMO_CATEGORY_ID,
                    level = 2,
                    name = context.getString(R.string.common_cat_gaming),
                    description = context.getString(R.string.common_cat_gaming_desc),
                    active = true,
                ),
                OrderCategoryEntity(
                    id = "demo-cat-rog-strix",
                    parentId = "demo-cat-gaming",
                    level = 3,
                    name = context.getString(R.string.common_cat_rog_strix),
                    description = context.getString(R.string.common_cat_rog_strix_desc),
                    active = true,
                ),
            ),
        )
        database.customerDao().upsertAll(
            (1..12).map { i ->
                CustomerEntity(
                    id = if (i == 1) DEMO_CUSTOMER_ID else "demo-customer-$i",
                    type = if ((i % 3) == 0) CustomerType.BUSINESS else CustomerType.PRIVATE,
                    firstName =
                        when {
                            i == 1 -> {
                                context.getString(R.string.demo_customer_first_name)
                            }

                            (i % 3) == 0 -> {
                                null
                            }

                            else -> {
                                testContext.getString(
                                    TestR.string.demo_customer_name,
                                    i,
                                )
                            }
                        },
                    lastName = if ((i % 3) == 0) null else context.getString(R.string.demo_customer_last_name),
                    companyName =
                        if ((i % 3) == 0) {
                            testContext.getString(
                                TestR.string.demo_customer_business_name,
                                i,
                            )
                        } else {
                            null
                        },
                    vatNumber = if ((i % 3) == 0) "US${10000000000L + i}" else null,
                    taxCode = if ((i % 3) == 0) null else "CF${1000000000000000L + i}",
                    phone = if (i == 1) context.getString(R.string.demo_customer_phone) else "+1 555-000-${1000 + i}",
                    email = if (i == 1) context.getString(R.string.demo_customer_email) else "customer$i@example.invalid",
                    address = if (i == 1) context.getString(R.string.demo_customer_address) else "$i Demo St, New York",
                    notes =
                        if (i == 1) {
                            context.getString(R.string.demo_customer_notes)
                        } else {
                            testContext.getString(
                                TestR.string.demo_customer_notes,
                                i,
                            )
                        },
                    createdAt = now - (i * 1000L),
                    updatedAt = now - (i * 1000L),
                )
            },
        )

        // Assign to Admin in Demo version, otherwise to Operator
        val assignedUserId = if (BuildConfig.IS_DEMO_VERSION) DEMO_ADMIN_ID else DEMO_OPERATOR_ID

        database.repairOrderDao().upsertAll(
            (1..50).map { i ->
                RepairOrderEntity(
                    id = if (i == 1) DEMO_ORDER_ID else "demo-order-$i",
                    orderNumber = yearPrefix + (1000 + i).toString().padStart(6, '0'),
                    customerId = DEMO_CUSTOMER_ID,
                    title =
                        if (i == 1) {
                            testContext.getString(TestR.string.demo_order_title_1)
                        } else {
                            testContext.getString(
                                TestR.string.demo_order_title_n,
                                i,
                            )
                        },
                    categoryId = "demo-cat-rog-strix",
                    status = if ((i % 2) == 0) OrderStatus.IN_PROGRESS else OrderStatus.OPEN,
                    priority = OrderPriority.NORMAL,
                    assignedToUserId = assignedUserId,
                    createdByUserId = assignedUserId,
                    description = testContext.getString(TestR.string.demo_order_desc, i),
                    internalNotes = testContext.getString(TestR.string.demo_order_notes, i),
                    estimateAmountCents = null,
                    finalAmountCents = null,
                    createdAt = now - (i * 3600000L),
                    updatedAt = now - (i * 3600000L),
                    closedAt = null,
                    closureReason = null,
                )
            },
        )

        val mediaPath =
            writeDemoBitmap(
                directory = mediaStorageProvider.getOrderMediaDir(DEMO_ORDER_ID),
                fileName = "demo-oggetto.png",
                label = testContext.getString(TestR.string.demo_photo_label),
                backgroundColor = Color.rgb(229, 241, 255),
            )
        database.orderMediaDao().upsert(
            OrderMediaEntity(
                id = "demo-media-intake",
                orderId = DEMO_ORDER_ID,
                mediaType = "PHOTO",
                localPath = mediaPath.absolutePath,
                fileName = mediaPath.name,
                mimeType = "image/png",
                sizeBytes = mediaPath.length(),
                width = 1280,
                height = 720,
                createdByUserId = assignedUserId,
                capturedAt = now + 1_000L,
                createdAt = now + 1_000L,
            ),
        )

        val mediaPath2 =
            writeDemoBitmap(
                directory = mediaStorageProvider.getOrderMediaDir(DEMO_ORDER_ID),
                fileName = "demo-dettaglio.png",
                label = testContext.getString(TestR.string.demo_photo_detail_label),
                backgroundColor = Color.rgb(240, 240, 240),
            )
        database.orderMediaDao().upsert(
            OrderMediaEntity(
                id = "demo-media-detail",
                orderId = DEMO_ORDER_ID,
                mediaType = "PHOTO",
                localPath = mediaPath2.absolutePath,
                fileName = mediaPath2.name,
                mimeType = "image/png",
                sizeBytes = mediaPath2.length(),
                width = 1280,
                height = 720,
                createdByUserId = assignedUserId,
                capturedAt = now + 5_000L,
                createdAt = now + 5_000L,
            ),
        )

        val signaturePath =
            writeDemoBitmap(
                directory = mediaStorageProvider.getOrderSignaturesDir(DEMO_ORDER_ID),
                fileName = "demo-firma.png",
                label = testContext.getString(TestR.string.demo_signature_label),
                backgroundColor = Color.WHITE,
            )
        database.signatureDao().upsert(
            SignatureEntity(
                id = "demo-signature-intake",
                orderId = DEMO_ORDER_ID,
                signerName = "John Doe",
                privacyAccepted = true,
                termsAccepted = true,
                privacyTextSnapshot = testContext.getString(TestR.string.demo_privacy_snapshot),
                privacyTemplateVersion = 1,
                termsTextSnapshot = testContext.getString(TestR.string.demo_terms_snapshot),
                termsTemplateVersion = 1,
                localPath = signaturePath.absolutePath,
                fileName = signaturePath.name,
                mimeType = "image/png",
                sizeBytes = signaturePath.length(),
                width = 1280,
                height = 720,
                createdByUserId = assignedUserId,
                signedAt = now + 2_000L,
                createdAt = now + 2_000L,
            ),
        )
        database.timelineEventDao().upsertAll(
            listOf(
                TimelineEventEntity(
                    id = "demo-timeline-created",
                    orderId = DEMO_ORDER_ID,
                    type = TimelineEventType.ORDER_CREATED,
                    title = testContext.getString(TestR.string.demo_event_title_created),
                    description = testContext.getString(TestR.string.demo_event_desc_created),
                    oldStatus = null,
                    newStatus = OrderStatus.OPEN.name,
                    visibleToCustomer = true,
                    requiresCustomerFeedback = false,
                    expectedFeedbackType = null,
                    customerFeedback = null,
                    customerFeedbackNote = null,
                    customerFeedbackAt = null,
                    createdByUserId = assignedUserId,
                    createdAt = now,
                ),
                TimelineEventEntity(
                    id = "demo-timeline-diagnosis",
                    orderId = DEMO_ORDER_ID,
                    type = TimelineEventType.STATUS_CHANGED,
                    title = testContext.getString(TestR.string.demo_event_title_diagnosis),
                    description = testContext.getString(TestR.string.demo_event_desc_diagnosis),
                    oldStatus = OrderStatus.IN_PROGRESS.name,
                    newStatus = OrderStatus.DIAGNOSIS.name,
                    visibleToCustomer = true,
                    requiresCustomerFeedback = false,
                    expectedFeedbackType = null,
                    customerFeedback = null,
                    customerFeedbackNote = null,
                    customerFeedbackAt = null,
                    createdByUserId = assignedUserId,
                    createdAt = now + 10_000L,
                ),
            ),
        )

        val videoPath = File(mediaStorageProvider.getOrderMediaDir(DEMO_ORDER_ID), "demo-video.mp4")
        if (!videoPath.exists()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                videoPath.createNewFile()
            }
        }
        database.orderMediaDao().upsert(
            OrderMediaEntity(
                id = "demo-media-video",
                orderId = DEMO_ORDER_ID,
                mediaType = "VIDEO",
                localPath = videoPath.absolutePath,
                fileName = videoPath.name,
                mimeType = "video/mp4",
                sizeBytes = 1024 * 512,
                width = 1280,
                height = 720,
                createdByUserId = assignedUserId,
                capturedAt = now + 15_000L,
                createdAt = now + 15_000L,
            ),
        )

        listOf(
            DemoTemplate(
                "privacy_short",
                context.getString(R.string.template_privacy_short_title),
                context.getString(R.string.template_privacy_short_body),
                context.getString(R.string.anchor_step_legal),
            ),
            DemoTemplate(
                "terms_intake",
                context.getString(R.string.template_terms_title),
                context.getString(R.string.template_terms_body),
                context.getString(R.string.anchor_step_legal),
            ),
            DemoTemplate(
                "whatsapp_intake",
                context.getString(R.string.template_wa_intake_title),
                context.getString(R.string.template_wa_intake_body),
                context.getString(R.string.anchor_step_intake),
            ),
            DemoTemplate(
                "whatsapp_update",
                context.getString(R.string.template_wa_update_title),
                context.getString(R.string.template_wa_update_body),
                context.getString(R.string.anchor_step_update),
            ),
            DemoTemplate(
                "whatsapp_diagnosis",
                context.getString(R.string.template_wa_diagnosis_title),
                context.getString(R.string.template_wa_diagnosis_body),
                context.getString(R.string.anchor_step_diagnosis),
            ),
            DemoTemplate(
                "whatsapp_quote",
                context.getString(R.string.template_wa_quote_title),
                context.getString(R.string.template_wa_quote_body),
                context.getString(R.string.anchor_step_quote),
            ),
            DemoTemplate(
                "whatsapp_not_repairable",
                context.getString(R.string.template_wa_not_repairable_title),
                context.getString(R.string.template_wa_not_repairable_body),
                context.getString(R.string.anchor_step_outcome),
            ),
            DemoTemplate(
                "whatsapp_repairing",
                context.getString(R.string.template_wa_repairing_title),
                context.getString(R.string.template_wa_repairing_body),
                context.getString(R.string.anchor_step_repair),
            ),
            DemoTemplate(
                "whatsapp_ready",
                context.getString(R.string.template_wa_ready_title),
                context.getString(R.string.template_wa_ready_body),
                context.getString(R.string.anchor_step_pickup),
            ),
            DemoTemplate(
                "whatsapp_pdf_status",
                context.getString(R.string.template_wa_pdf_status_title),
                context.getString(R.string.template_wa_pdf_status_body),
                context.getString(R.string.anchor_step_docs),
            ),
            DemoTemplate(
                "whatsapp_completed",
                context.getString(R.string.template_wa_completed_title),
                context.getString(R.string.template_wa_completed_body),
                context.getString(R.string.anchor_step_closed),
            ),
        ).forEachIndexed { index, template ->
            database.messageTemplateDao().upsert(
                MessageTemplateEntity(
                    id = "demo-template-$index",
                    code = template.code,
                    title = template.title,
                    channel = MessageChannel.WHATSAPP,
                    body = template.body,
                    active = true,
                    version = 1,
                    updatedByUserId = DEMO_ADMIN_ID,
                    updatedAt = now,
                    step = template.step,
                ),
            )
        }

        // Reset Anchor Mappings to defaults
        database.appSettingDao().deleteByKeys(
            com.onlytechtake.repairapp.core.templates.TemplateAnchor.entries
                .map { it.settingKey },
        )

        AuthPreferences(context).markConfigured()
    }

    private data class DemoTemplate(
        val code: String,
        val title: String,
        val body: String,
        val step: String,
    )

    private fun writeDemoBitmap(
        directory: File,
        fileName: String,
        label: String,
        backgroundColor: Int,
    ): File {
        directory.mkdirs()
        val file = File(directory, fileName)
        val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(backgroundColor)
        paint.color = Color.rgb(31, 41, 55)
        paint.textSize = 54f
        canvas.drawText(label, 96f, 160f, paint)
        paint.textSize = 36f
        canvas.drawText("OnlyTechTake", 96f, 230f, paint)
        paint.color = Color.rgb(37, 99, 235)
        paint.strokeWidth = 8f
        canvas.drawLine(96f, 300f, 880f, 560f, paint)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return file
    }
}
