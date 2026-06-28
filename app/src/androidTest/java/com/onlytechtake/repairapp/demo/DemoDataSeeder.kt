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
    const val DEMO_OPERATOR_USERNAME = "operatore.demo"
    private const val DEMO_ADMIN_ID = "demo-admin"
    private const val DEMO_OPERATOR_ID = "demo-operator"
    const val DEMO_CATEGORY_ID = "demo-category-notebook"
    private const val DEMO_CUSTOMER_ID = "demo-customer"
    const val DEMO_ORDER_ID = "demo-order-operator-intake"

    private val testContext: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    suspend fun seedDemoProfile(context: Context) {
        // Force English for consistent assets
        val locale = java.util.Locale.ENGLISH
        java.util.Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        RepairAppDatabase.closeInstance()
        context.deleteDatabase(RepairAppDatabase.DATABASE_NAME)
        AuthPreferences(context).clear()
        val ownerPreferences = OwnerPreferences(context)
        ownerPreferences.updateOwnerInfo(
            OwnerInfo(
                name = "OnlyTechTake Repair Lab",
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
                    name = testContext.getString(TestR.string.common_cat_pc),
                    description = testContext.getString(TestR.string.common_cat_pc_desc),
                    active = true,
                ),
                OrderCategoryEntity(
                    id = DEMO_CATEGORY_ID, // notebook
                    parentId = "demo-cat-pc",
                    level = 1,
                    name = testContext.getString(TestR.string.common_cat_notebook),
                    description = testContext.getString(TestR.string.common_cat_notebook_desc),
                    active = true,
                ),
                OrderCategoryEntity(
                    id = "demo-cat-gaming",
                    parentId = DEMO_CATEGORY_ID,
                    level = 2,
                    name = testContext.getString(TestR.string.common_cat_gaming),
                    description = testContext.getString(TestR.string.common_cat_gaming_desc),
                    active = true,
                ),
                OrderCategoryEntity(
                    id = "demo-cat-rog-strix",
                    parentId = "demo-cat-gaming",
                    level = 3,
                    name = testContext.getString(TestR.string.common_cat_rog_strix),
                    description = testContext.getString(TestR.string.common_cat_rog_strix_desc),
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
                                "John"
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
                    lastName = if ((i % 3) == 0) null else "Doe",
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
                    phone = "+1 555-000-${1000 + i}",
                    email = "customer$i@example.invalid",
                    address = "$i Demo St, New York",
                    notes = testContext.getString(TestR.string.demo_customer_notes, i),
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
        database.timelineEventDao().upsert(
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
        )

        listOf(
            DemoTemplate(
                "privacy_breve",
                context.getString(R.string.template_privacy_breve_title),
                context.getString(R.string.anchor_step_legal),
            ),
            DemoTemplate(
                "condizioni_presa_in_carico",
                context.getString(R.string.template_condizioni_title),
                context.getString(R.string.anchor_step_legal),
            ),
            DemoTemplate(
                "whatsapp_presa_in_carico",
                context.getString(R.string.template_wa_intake_title),
                context.getString(R.string.anchor_step_intake),
            ),
            DemoTemplate(
                "whatsapp_aggiornamento",
                context.getString(R.string.template_wa_update_title),
                context.getString(R.string.anchor_step_update),
            ),
            DemoTemplate(
                "whatsapp_diagnosi",
                context.getString(R.string.template_wa_diagnosis_title),
                context.getString(R.string.anchor_step_diagnosis),
            ),
            DemoTemplate(
                "whatsapp_risultato",
                context.getString(R.string.template_wa_result_title),
                context.getString(R.string.anchor_step_outcome),
            ),
            DemoTemplate(
                "whatsapp_preventivo_intervento",
                context.getString(R.string.template_wa_quote_title),
                context.getString(R.string.anchor_step_quote),
            ),
            DemoTemplate(
                "whatsapp_non_riparabile",
                context.getString(R.string.template_wa_not_repairable_title),
                context.getString(R.string.anchor_step_outcome),
            ),
            DemoTemplate(
                "whatsapp_riparazione",
                context.getString(R.string.template_wa_repairing_title),
                context.getString(R.string.anchor_step_repair),
            ),
            DemoTemplate(
                "whatsapp_pronto_ritiro",
                context.getString(R.string.template_wa_ready_title),
                context.getString(R.string.anchor_step_pickup),
            ),
            DemoTemplate(
                "whatsapp_stato_avanzamento_pdf",
                context.getString(R.string.template_wa_pdf_status_title),
                context.getString(R.string.anchor_step_docs),
            ),
        ).forEachIndexed { index, template ->
            database.messageTemplateDao().upsert(
                MessageTemplateEntity(
                    id = "demo-template-$index",
                    code = template.code,
                    title = template.title,
                    channel = MessageChannel.WHATSAPP,
                    body = testContext.getString(TestR.string.demo_template_body),
                    active = true,
                    version = 1,
                    updatedByUserId = DEMO_ADMIN_ID,
                    updatedAt = now,
                    step = template.step,
                ),
            )
        }
        AuthPreferences(context).markConfigured()
    }

    private data class DemoTemplate(
        val code: String,
        val title: String,
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
        canvas.drawText("OnlyTechTake Repair App", 96f, 230f, paint)
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
