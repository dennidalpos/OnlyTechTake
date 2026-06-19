package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.room.withTransaction
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

object ShowcaseDataSeeder {
    const val DEFAULT_PASSWORD = "ShowcasePassword123"

    suspend fun seedShowcaseData(context: Context) {
        val authPreferences = AuthPreferences(context)

        // Clean up before seeding
        RepairAppDatabase.closeInstance()
        context.deleteDatabase(RepairAppDatabase.DATABASE_NAME)

        // IMPORTANT: delete WAL files as well just in case
        context.deleteDatabase("${RepairAppDatabase.DATABASE_NAME}-wal")
        context.deleteDatabase("${RepairAppDatabase.DATABASE_NAME}-shm")

        authPreferences.clear()

        val mediaStorageProvider = MediaStorageProvider(context)
        mediaStorageProvider.getBaseMediaDir().deleteRecursively()

        // Re-get instance after deletion
        val db = RepairAppDatabase.getInstance(context)

        val now = System.currentTimeMillis()
        val passwordHasher = PasswordHasher()
        val pwdHash = passwordHasher.hash(DEFAULT_PASSWORD.toCharArray())

        // Create bitmap resources once
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Use transaction for all DB operations
        db.withTransaction {
            // 1. Create Operators (3 with different permissions)
            val operators =
                listOf(
                    UserEntity(
                        id = "op-reception",
                        displayName = "Anna Receptionist",
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
                        displayName = "Marco Tecnico",
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
                        displayName = "Elena Manager",
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
                        displayName = "Admin Showcase",
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
                    // Smartphone branch (4 levels)
                    OrderCategoryEntity(
                        "cat-smartphone",
                        null,
                        0,
                        "Smartphone",
                        "Riparazione telefoni cellulari",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-apple",
                        "cat-smartphone",
                        1,
                        "Apple",
                        "Dispositivi iOS",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-iphone",
                        "cat-apple",
                        2,
                        "iPhone",
                        "Tutti i modelli iPhone",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-iphone-14-pro",
                        "cat-iphone",
                        3,
                        "iPhone 14 Pro",
                        "Riparazione display e vetro posteriore",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-iphone-13",
                        "cat-iphone",
                        3,
                        "iPhone 13",
                        "Sostituzione batteria e connettore",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-samsung",
                        "cat-smartphone",
                        1,
                        "Samsung",
                        "Dispositivi Android Galaxy",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-galaxy-s",
                        "cat-samsung",
                        2,
                        "Serie Galaxy S",
                        "Fascia alta Samsung",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-s23-ultra",
                        "cat-galaxy-s",
                        3,
                        "S23 Ultra",
                        "Riparazione schermo curvo e S-Pen",
                        true,
                    ),
                    // Computer branch (4 levels)
                    OrderCategoryEntity(
                        "cat-pc",
                        null,
                        0,
                        "Computer & Laptop",
                        "Riparazioni hardware e software PC",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-notebook",
                        "cat-pc",
                        1,
                        "Notebook",
                        "Portatili e Ultrabook",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-gaming-laptop",
                        "cat-notebook",
                        2,
                        "Gaming Laptop",
                        "MSI, ROG, Alienware, Legion",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-rog-strix",
                        "cat-gaming-laptop",
                        3,
                        "Asus ROG Strix",
                        "Pulizia interna e pasta termica",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-windows-laptop",
                        "cat-notebook",
                        2,
                        "Windows Laptop",
                        "Asus, HP, Lenovo, Dell",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-asus-zenbook",
                        "cat-windows-laptop",
                        3,
                        "Asus Zenbook OLED",
                        "Riparazione cerniere e display",
                        true,
                    ),
                    // Console branch (4 levels)
                    OrderCategoryEntity(
                        "cat-console",
                        null,
                        0,
                        "Console Game",
                        "Riparazioni PlayStation, Xbox, Switch",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-sony",
                        "cat-console",
                        1,
                        "Sony",
                        "Ecosistema PlayStation",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-ps5",
                        "cat-sony",
                        2,
                        "PlayStation 5",
                        "Revisione e riparazione PS5",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-ps5-hdmi",
                        "cat-ps5",
                        3,
                        "Sostituzione Porta HDMI",
                        "Sostituzione connettore video 4K",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-dualsense",
                        "cat-ps5",
                        3,
                        "DualSense Controller",
                        "Riparazione Analogico Drift e Tasti",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-tablet",
                        null,
                        0,
                        "Tablet",
                        "Riparazioni iPad e Android",
                        true,
                    ),
                    OrderCategoryEntity("cat-ipad", "cat-tablet", 1, "iPad", "Tablet Apple", true),
                    OrderCategoryEntity(
                        "cat-ipad-pro",
                        "cat-ipad",
                        2,
                        "iPad Pro",
                        "Modelli 11\" e 12.9\"",
                        true,
                    ),
                    OrderCategoryEntity(
                        "cat-ipad-glass",
                        "cat-ipad-pro",
                        3,
                        "Sostituzione Vetro/LCD",
                        "Riparazione pannello touch",
                        true,
                    ),
                )
            db.orderCategoryDao().upsertAll(categories)

            // 3. Create Private Customers (5)
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
                        notes = "Cliente privato di test.",
                        createdAt = now - (i * 86400000L),
                        updatedAt = now - (i * 86400000L),
                    )
                }
            db.customerDao().upsertAll(privateCustomers)

            // 4. Create Business Customers (5)
            val businessCustomers =
                (1..5).map { i ->
                    CustomerEntity(
                        id = "cust-b-$i",
                        type = CustomerType.BUSINESS,
                        firstName = null,
                        lastName = null,
                        companyName =
                            listOf(
                                "Tech Solutions Srl",
                                "Global Food SpA",
                                "Studio Legale Rossi",
                                "Officina Meccanica",
                                "Bar Centrale",
                            )[i - 1],
                        vatNumber = "0123456789$i",
                        taxCode = "BCN${UUID.randomUUID().toString().take(13)}",
                        phone = "+39 02 555 666$i",
                        email = "info@business$i.example.com",
                        address = "Piazza Affari $i, Milano",
                        notes = "Cliente aziendale di test.",
                        createdAt = now - (i * 86400000L),
                        updatedAt = now - (i * 86400000L),
                    )
                }
            db.customerDao().upsertAll(businessCustomers)

            // 5. Create Orders for each customer
            val allCustomers = privateCustomers + businessCustomers
            val statuses = OrderStatus.entries.toTypedArray()

            // Select leaf categories for orders
            val selectableCategories =
                categories.filter { cat -> categories.none { it.parentId == cat.id } }

            var orderCount = 1
            allCustomers.forEach { customer ->
                // Create 3 orders for each customer: 2 active and 1 closed
                repeat(3) { j ->
                    val orderId = "order-$orderCount"
                    val isClosed = (j == 2)
                    val status =
                        if (isClosed) OrderStatus.CLOSED else statuses[(orderCount % (statuses.size - 1))]
                    val categoryId =
                        selectableCategories[(orderCount % selectableCategories.size)].id
                    val operatorId = operators[(orderCount % 3)].id

                    val order =
                        RepairOrderEntity(
                            id = orderId,
                            orderNumber = "2026-${orderCount.toString().padStart(4, '0')}",
                            customerId = customer.id,
                            title = "Riparazione ${
                                listOf(
                                    "PC",
                                    "Smartphone",
                                    "Tablet",
                                    "Console",
                                )[orderCount % 4]
                            } - Modello $j",
                            categoryId = categoryId,
                            status = status,
                            priority = OrderPriority.NORMAL,
                            assignedToUserId = operatorId,
                            createdByUserId = "op-reception",
                            description = "Problema riscontrato: il dispositivo non funziona correttamente.",
                            internalNotes = "Nota interna di test.",
                            estimateAmountCents = if (isClosed) 5000L else null,
                            finalAmountCents = if (isClosed) 4500L else null,
                            diagnosisOutcome = if (isClosed) DiagnosisOutcome.REPAIRABLE else null,
                            createdAt = now - (orderCount * 3600000L * 2),
                            updatedAt = now - (orderCount * 3600000L),
                            closedAt = if (isClosed) now - (orderCount * 1800000L) else null,
                            closureReason = if (isClosed) ClosureReason.COMPLETED else null,
                        )
                    db.repairOrderDao().upsert(order)

                    // Add Timeline Event
                    db.timelineEventDao().upsert(
                        TimelineEventEntity(
                            id = "event-$orderId-init",
                            orderId = orderId,
                            type = TimelineEventType.ORDER_CREATED,
                            title = "Presa in carico",
                            description = "Dispositivo ricevuto al banco.",
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
                                title = "Commessa Chiusa",
                                description = "Riparazione completata e consegnata.",
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

                    // Add Sample Photo
                    val photoFile =
                        writeDemoBitmap(
                            directory = mediaStorageProvider.getOrderMediaDir(orderId),
                            fileName = "photo-$orderId.png",
                            label = "Oggetto: ${order.title}",
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

                    // Add Sample Signature
                    val sigFile =
                        writeDemoBitmap(
                            directory = mediaStorageProvider.getOrderSignaturesDir(orderId),
                            fileName = "sig-$orderId.png",
                            label = "Firma: ${customer.firstName ?: customer.companyName}",
                            backgroundColor = Color.WHITE,
                            paint = paint,
                        )
                    db.signatureDao().upsert(
                        SignatureEntity(
                            id = "sig-$orderId",
                            orderId = orderId,
                            signerName = customer.firstName ?: customer.companyName ?: "Cliente",
                            privacyAccepted = true,
                            termsAccepted = true,
                            privacyTextSnapshot = "Informativa Privacy Standard",
                            privacyTemplateVersion = 1,
                            termsTextSnapshot = "Termini e Condizioni di Riparazione",
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

            // 6. Create Default Templates
            val templates =
                listOf(
                    "privacy" to "Informativa privacy",
                    "terms" to "Condizioni presa in carico",
                    "whatsapp_presa_in_carico" to "Presa in carico",
                    "whatsapp_lavorazione" to "Aggiornamento lavorazione",
                    "whatsapp_pronto_ritiro" to "Pronto ritiro",
                ).mapIndexed { index, (code, title) ->
                    MessageTemplateEntity(
                        id = "template-$index",
                        code = code,
                        title = title,
                        channel = MessageChannel.WHATSAPP,
                        body = "Ciao {{cliente}}, la tua commessa {{numero_commessa}} è ora in stato {{stato_commessa}}.",
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
