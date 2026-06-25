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
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Showcase implementation of [DataSeeder].
 * Populates the database with a rich set of demo data for quick presentations.
 */
class DataSeederImpl : DataSeeder {
    override suspend fun seed(context: Context) {
        ShowcaseDataSeeder.seedShowcaseData(context)
    }

    override val defaultPassword: String = ShowcaseDataSeeder.DEFAULT_PASSWORD
}

object ShowcaseDataSeeder {
    const val DEFAULT_PASSWORD = "ShowcasePassword123"

    suspend fun seedShowcaseData(context: Context) {
        val authPreferences = AuthPreferences(context)
        val mediaStorageProvider = MediaStorageProvider(context)

        val db = RepairAppDatabase.getInstance(context)
        val now = System.currentTimeMillis()
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
                            ),
                        active = true,
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
                            ),
                        active = true,
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
                        context.getString(R.string.showcase_cat_smartphone),
                        context.getString(R.string.showcase_cat_smartphone_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-apple",
                        "cat-smartphone",
                        1,
                        "Apple",
                        context.getString(R.string.showcase_cat_apple_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-iphone",
                        "cat-apple",
                        2,
                        "iPhone",
                        context.getString(R.string.showcase_cat_iphone_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-iphone-14-pro",
                        "cat-iphone",
                        3,
                        "iPhone 14 Pro",
                        context.getString(R.string.showcase_cat_iphone_14_pro_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-iphone-13",
                        "cat-iphone",
                        3,
                        "iPhone 13",
                        context.getString(R.string.showcase_cat_iphone_13_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-samsung",
                        "cat-smartphone",
                        1,
                        "Samsung",
                        context.getString(R.string.showcase_cat_samsung_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-galaxy-s",
                        "cat-samsung",
                        2,
                        context.getString(R.string.showcase_cat_galaxy_s),
                        context.getString(R.string.showcase_cat_galaxy_s_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-s23-ultra",
                        "cat-galaxy-s",
                        3,
                        "S23 Ultra",
                        context.getString(R.string.showcase_cat_s23_ultra_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-pc",
                        null,
                        0,
                        context.getString(R.string.showcase_cat_pc),
                        context.getString(R.string.showcase_cat_pc_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-notebook",
                        "cat-pc",
                        1,
                        "Notebook",
                        context.getString(R.string.showcase_cat_notebook_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-gaming-laptop",
                        "cat-notebook",
                        2,
                        context.getString(R.string.showcase_cat_gaming_laptop),
                        context.getString(R.string.showcase_cat_gaming_laptop_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-rog-strix",
                        "cat-gaming-laptop",
                        3,
                        "Asus ROG Strix",
                        context.getString(R.string.showcase_cat_rog_strix_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-windows-laptop",
                        "cat-notebook",
                        2,
                        context.getString(R.string.showcase_cat_windows_laptop),
                        context.getString(R.string.showcase_cat_windows_laptop_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-asus-zenbook",
                        "cat-windows-laptop",
                        3,
                        "Asus Zenbook OLED",
                        context.getString(R.string.showcase_cat_asus_zenbook_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-console",
                        null,
                        0,
                        context.getString(R.string.showcase_cat_console),
                        context.getString(R.string.showcase_cat_console_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-sony",
                        "cat-console",
                        1,
                        "Sony",
                        context.getString(R.string.showcase_cat_sony_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-ps5",
                        "cat-sony",
                        2,
                        "PlayStation 5",
                        context.getString(R.string.showcase_cat_ps5_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-ps5-hdmi",
                        "cat-ps5",
                        3,
                        context.getString(R.string.showcase_cat_ps5_hdmi),
                        context.getString(R.string.showcase_cat_ps5_hdmi_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-dualsense",
                        "cat-ps5",
                        3,
                        "DualSense Controller",
                        context.getString(R.string.showcase_cat_dualsense_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-tablet",
                        null,
                        0,
                        "Tablet",
                        context.getString(R.string.showcase_cat_tablet_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-ipad",
                        "cat-tablet",
                        1,
                        "iPad",
                        context.getString(R.string.showcase_cat_ipad_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-ipad-pro",
                        "cat-ipad",
                        2,
                        "iPad Pro",
                        context.getString(R.string.showcase_cat_ipad_pro_desc),
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-ipad-glass",
                        "cat-ipad-pro",
                        3,
                        context.getString(R.string.showcase_cat_ipad_glass),
                        context.getString(R.string.showcase_cat_ipad_glass_desc),
                        true,
                    ),
                )
            db.orderCategoryDao().upsertAll(categories)

            // 3. Create Private Customers
            val privateCustomers =
                (1..5).map { i ->
                    CustomerEntity(
                        id = "cust-p-$i",
                        type = CustomerType.PRIVATE,
                        firstName =
                            listOf(
                                "Mario",
                                "Luigi",
                                "Giovanna",
                                "Francesca",
                                "Roberto",
                            )[i - 1],
                        lastName = listOf("Rossi", "Bianchi", "Verdi", "Neri", "Gialli")[i - 1],
                        companyName = null,
                        vatNumber = null,
                        taxCode = "CUSTPRV$i${UUID.randomUUID().toString().take(5)}",
                        phone = "+39 333 100 200$i",
                        email = "customer.p$i@example.com",
                        address = "Via Roma $i, Milano",
                        notes = context.getString(R.string.showcase_customer_notes),
                        createdAt = now - (i * 86400000L),
                        updatedAt = now - (i * 86400000L),
                    )
                }
            db.customerDao().upsertAll(privateCustomers)

            // 4. Create Business Customers
            val businessCustomers =
                (1..5).map { i ->
                    CustomerEntity(
                        id = "cust-b-$i",
                        type = CustomerType.BUSINESS,
                        firstName = null,
                        lastName = null,
                        companyName =
                            listOf(
                                context.getString(R.string.showcase_business_name_1),
                                context.getString(R.string.showcase_business_name_2),
                                context.getString(R.string.showcase_business_name_3),
                                context.getString(R.string.showcase_business_name_4),
                                context.getString(R.string.showcase_business_name_5),
                            )[i - 1],
                        vatNumber = "0123456789$i",
                        taxCode = "BCN${UUID.randomUUID().toString().take(13)}",
                        phone = "+39 02 555 666$i",
                        email = "info@business$i.example.com",
                        address = "Piazza Affari $i, Milano",
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

            var orderCount = 1
            allCustomers.forEach { customer ->
                repeat(3) { j ->
                    val orderId = "order-$orderCount"
                    val isClosed = (j == 2)
                    val status =
                        if (isClosed) OrderStatus.CLOSED else statuses[(orderCount % (statuses.size - 1))]
                    val categoryId =
                        selectableCategories[(orderCount % selectableCategories.size)].id
                    val operatorId = operators[(orderCount % 3)].id
                    val repairType = listOf("PC", "Smartphone", "Tablet", "Console")[orderCount % 4]

                    val order =
                        RepairOrderEntity(
                            id = orderId,
                            orderNumber = "2026-${orderCount.toString().padStart(4, '0')}",
                            customerId = customer.id,
                            title = context.getString(R.string.showcase_order_title, repairType, j),
                            categoryId = categoryId,
                            status = status,
                            priority = OrderPriority.NORMAL,
                            assignedToUserId = operatorId,
                            createdByUserId = "op-reception",
                            description = context.getString(R.string.showcase_order_description),
                            internalNotes = context.getString(R.string.showcase_order_internal_notes),
                            estimateAmountCents = if (isClosed) 5000L else null,
                            finalAmountCents = if (isClosed) 4500L else null,
                            diagnosisOutcome = if (isClosed) DiagnosisOutcome.REPAIRABLE else null,
                            createdAt = now - (orderCount * 3600000L * 2),
                            updatedAt = now - (orderCount * 3600000L),
                            closedAt = if (isClosed) now - (orderCount * 1800000L) else null,
                            closureReason = if (isClosed) ClosureReason.COMPLETED else null,
                        )
                    db.repairOrderDao().upsert(order)

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
                            directory = mediaStorageProvider.getOrderMediaDir(orderId),
                            fileName = "photo-$orderId.png",
                            label = context.getString(R.string.showcase_photo_label, order.title),
                            backgroundColor = Color.LTGRAY,
                            paint = paint,
                        )
                    db.orderMediaDao().upsert(
                        OrderMediaEntity(
                            id = "media-$orderId",
                            orderId = orderId,
                            mediaType = "PHOTO",
                            localPath = photoFile.absolutePath,
                            fileName = photoFile.name,
                            mimeType = "image/png",
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
                            directory = mediaStorageProvider.getOrderSignaturesDir(orderId),
                            fileName = "sig-$orderId.png",
                            label =
                                context.getString(
                                    R.string.showcase_sig_label,
                                    customer.firstName ?: customer.companyName,
                                ),
                            backgroundColor = Color.WHITE,
                            paint = paint,
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
                            mimeType = "image/png",
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
            val templates =
                listOf(
                    "privacy_breve" to context.getString(R.string.template_privacy_breve_title) to
                        context.getString(
                            R.string.template_privacy_breve_body,
                        ),
                    "condizioni_presa_in_carico" to context.getString(R.string.template_condizioni_title) to
                        context.getString(
                            R.string.template_condizioni_body,
                        ),
                    "whatsapp_presa_in_carico" to context.getString(R.string.template_wa_intake_title) to
                        context.getString(
                            R.string.template_wa_intake_body,
                        ),
                    "whatsapp_aggiornamento" to context.getString(R.string.template_wa_update_title) to
                        context.getString(
                            R.string.template_wa_update_body,
                        ),
                    "whatsapp_diagnosi" to context.getString(R.string.template_wa_diagnosis_title) to
                        context.getString(
                            R.string.template_wa_diagnosis_body,
                        ),
                    "whatsapp_risultato" to context.getString(R.string.template_wa_result_title) to
                        context.getString(
                            R.string.template_wa_result_body,
                        ),
                    "whatsapp_preventivo_intervento" to context.getString(R.string.template_wa_quote_title) to
                        context.getString(
                            R.string.template_wa_quote_body,
                        ),
                    "whatsapp_non_riparabile" to context.getString(R.string.template_wa_not_repairable_title) to
                        context.getString(
                            R.string.template_wa_not_repairable_body,
                        ),
                    "whatsapp_riparazione" to context.getString(R.string.template_wa_repairing_title) to
                        context.getString(
                            R.string.template_wa_repairing_body,
                        ),
                    "whatsapp_lavorazione" to context.getString(R.string.template_wa_lavorazione_title) to
                        context.getString(
                            R.string.template_wa_lavorazione_body,
                        ),
                    "whatsapp_completamento" to context.getString(R.string.template_wa_completamento_title) to
                        context.getString(
                            R.string.template_wa_completamento_body,
                        ),
                    "whatsapp_pronto_ritiro" to context.getString(R.string.template_wa_ready_title) to
                        context.getString(
                            R.string.template_wa_ready_body,
                        ),
                    "whatsapp_stato_avanzamento_pdf" to context.getString(R.string.template_wa_pdf_status_title) to
                        context.getString(
                            R.string.template_wa_pdf_status_body,
                        ),
                ).mapIndexed { index, pair ->
                    val (codeTitle, body) = pair
                    val (code, title) = codeTitle
                    MessageTemplateEntity(
                        id = "template-$index",
                        code = code,
                        title = title,
                        channel = MessageChannel.WHATSAPP,
                        body = body,
                        active = true,
                        version = 1,
                        updatedByUserId = "admin-showcase",
                        updatedAt = now,
                    )
                }
            db.messageTemplateDao().upsertAll(templates)
        }

        authPreferences.markConfigured()
    }

    private fun writeDemoBitmap(
        directory: File,
        fileName: String,
        label: String,
        backgroundColor: Int,
        paint: Paint,
    ): File {
        directory.mkdirs()
        val file = File(directory, fileName)
        val bitmap = createBitmap(1280, 720)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)
        paint.color = Color.BLACK
        paint.textSize = 40f
        canvas.drawText(label, 50f, 100f, paint)
        paint.textSize = 24f
        canvas.drawText("Showcase Data - OnlyTechTake", 50f, 150f, paint)

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
