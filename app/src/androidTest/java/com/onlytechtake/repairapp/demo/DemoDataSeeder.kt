package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import java.io.File
import java.io.FileOutputStream

object DemoDataSeeder {
    const val DEMO_PASSWORD = "DemoPass123"
    const val DEMO_ADMIN_USERNAME = "admin.demo"
    const val DEMO_OPERATOR_USERNAME = "operatore.demo"
    const val DEMO_ADMIN_ID = "demo-admin"
    const val DEMO_OPERATOR_ID = "demo-operator"
    const val DEMO_CATEGORY_ID = "demo-category-notebook"
    const val DEMO_CUSTOMER_ID = "demo-customer"
    const val DEMO_ORDER_ID = "demo-order-operator-intake"

    suspend fun seedDemoProfile(context: Context) {
        RepairAppDatabase.closeInstance()
        context.deleteDatabase(RepairAppDatabase.DATABASE_NAME)
        AuthPreferences(context).clear()
        File(context.filesDir, "demo-media").deleteRecursively()

        val now = 1_780_830_000_000L
        val passwordHasher = PasswordHasher()
        val adminHash = passwordHasher.hash(DEMO_PASSWORD.toCharArray())
        val operatorHash = passwordHasher.hash(DEMO_PASSWORD.toCharArray())
        val database = RepairAppDatabase.getInstance(context)

        database.userDao().upsert(
            UserEntity(
                id = DEMO_ADMIN_ID,
                displayName = "Admin Demo",
                username = DEMO_ADMIN_USERNAME,
                passwordHash = adminHash.hash,
                passwordSalt = adminHash.salt,
                role = UserRole.ADMIN,
                active = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.userDao().upsert(
            UserEntity(
                id = DEMO_OPERATOR_ID,
                displayName = "Operatore Banco",
                username = DEMO_OPERATOR_USERNAME,
                passwordHash = operatorHash.hash,
                passwordSalt = operatorHash.salt,
                role = UserRole.OPERATOR,
                permissions = setOf(
                    UserPermission.CREATE_ORDERS,
                    UserPermission.EDIT_OWN_ORDERS,
                    UserPermission.CLOSE_ORDERS,
                    UserPermission.MANAGE_CUSTOMERS,
                    UserPermission.MANAGE_PAYMENTS
                ),
                active = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.orderCategoryDao().upsert(
            OrderCategoryEntity(
                id = DEMO_CATEGORY_ID,
                parentId = null,
                level = 0,
                name = "Notebook",
                description = "Diagnosi e riparazioni notebook e ultrabook.",
                active = true,
            ),
        )
        database.customerDao().upsert(
            CustomerEntity(
                id = DEMO_CUSTOMER_ID,
                type = CustomerType.PRIVATE,
                firstName = "Marco",
                lastName = "Verdi",
                companyName = null,
                vatNumber = null,
                taxCode = "VRDMRC80A01H501U",
                phone = "+39 333 000 1122",
                email = "marco.verdi@example.invalid",
                address = "Via Demo 12, Milano",
                notes = "Cliente demo per presentazioni.",
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.repairOrderDao().upsert(
            RepairOrderEntity(
                id = DEMO_ORDER_ID,
                orderNumber = "2026-0001",
                customerId = DEMO_CUSTOMER_ID,
                title = "Notebook HP - non si accende",
                categoryId = DEMO_CATEGORY_ID,
                status = OrderStatus.OPEN,
                priority = OrderPriority.NORMAL,
                assignedToUserId = DEMO_OPERATOR_ID,
                createdByUserId = DEMO_OPERATOR_ID,
                description = "Notebook ricevuto al banco: non si accende dopo caduta accidentale.",
                internalNotes = "Controllare alimentatore, connettore di ricarica e scheda madre.",
                estimateAmountCents = null,
                finalAmountCents = null,
                createdAt = now,
                updatedAt = now,
                closedAt = null,
                closureReason = null,
            ),
        )

        val mediaPath = writeDemoBitmap(context, "demo-oggetto.png", "Foto oggetto", Color.rgb(229, 241, 255))
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
                createdByUserId = DEMO_OPERATOR_ID,
                capturedAt = now + 1_000L,
                createdAt = now + 1_000L,
            ),
        )

        val signaturePath = writeDemoBitmap(context, "demo-firma.png", "Firma cliente", Color.WHITE)
        database.signatureDao().upsert(
            SignatureEntity(
                id = "demo-signature-intake",
                orderId = DEMO_ORDER_ID,
                signerName = "Marco Verdi",
                privacyAccepted = true,
                termsAccepted = true,
                privacyTextSnapshot = "Informativa privacy demo.",
                privacyTemplateVersion = 1,
                termsTextSnapshot = "Condizioni presa in carico demo.",
                termsTemplateVersion = 1,
                localPath = signaturePath.absolutePath,
                fileName = signaturePath.name,
                mimeType = "image/png",
                sizeBytes = signaturePath.length(),
                width = 1280,
                height = 720,
                createdByUserId = DEMO_OPERATOR_ID,
                signedAt = now + 2_000L,
                createdAt = now + 2_000L,
            ),
        )
        database.timelineEventDao().upsert(
            TimelineEventEntity(
                id = "demo-timeline-created",
                orderId = DEMO_ORDER_ID,
                type = TimelineEventType.ORDER_CREATED,
                title = "Commessa creata",
                description = "Presa in carico demo con foto e firma cliente.",
                oldStatus = null,
                newStatus = OrderStatus.OPEN.name,
                visibleToCustomer = true,
                requiresCustomerFeedback = false,
                expectedFeedbackType = null,
                customerFeedback = null,
                customerFeedbackNote = null,
                customerFeedbackAt = null,
                createdByUserId = DEMO_OPERATOR_ID,
                createdAt = now,
            ),
        )

        listOf(
            "privacy" to "Informativa privacy",
            "terms" to "Condizioni presa in carico",
            "whatsapp_presa_in_carico" to "Presa in carico",
            "whatsapp_lavorazione" to "Aggiornamento lavorazione",
            "whatsapp_diagnosi" to "Diagnosi",
            "whatsapp_risultato" to "Risultato diagnosi",
            "whatsapp_riparazione" to "Riparazione",
            "whatsapp_pronto_ritiro" to "Pronto ritiro",
        ).forEachIndexed { index, (code, title) ->
            database.messageTemplateDao().upsert(
                MessageTemplateEntity(
                    id = "demo-template-$index",
                    code = code,
                    title = title,
                    channel = MessageChannel.WHATSAPP,
                    body = "Aggiornamento {{orderNumber}}: {{orderStatus}} per {{customerName}}.",
                    active = true,
                    version = 1,
                    updatedByUserId = DEMO_ADMIN_ID,
                    updatedAt = now,
                ),
            )
        }
        AuthPreferences(context).markConfigured()
    }

    private fun writeDemoBitmap(context: Context, fileName: String, label: String, backgroundColor: Int): File {
        val directory = File(context.filesDir, "demo-media").apply { mkdirs() }
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
