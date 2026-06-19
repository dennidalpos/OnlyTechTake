package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.security.DatabaseKeyManager
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import com.onlytechtake.repairapp.data.local.entities.ClosureReason
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthPolicy
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.auth.LoginResult
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.DiagnosisResultInput
import com.onlytechtake.repairapp.domain.workspace.FeedbackInput
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.domain.workspace.RepairOrderInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class RepositoryPathsInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: RepairAppDatabase
    private lateinit var authPreferences: AuthPreferences

    @Before
    fun setUp() =
        runBlocking {
            context = ApplicationProvider.getApplicationContext()
            val key = DatabaseKeyManager(context).getDatabaseKey()
            val factory = SupportOpenHelperFactory(key.toByteArray())
            database =
                Room
                    .inMemoryDatabaseBuilder(context, RepairAppDatabase::class.java)
                    .openHelperFactory(factory)
                    .allowMainThreadQueries()
                    .build()
            authPreferences = AuthPreferences(context)
            authPreferences.clear()
            clearAppFiles()
        }

    @After
    fun tearDown() =
        runBlocking {
            authPreferences.clear()
            database.close()
            clearAppFiles()
        }

    @Test
    fun authLockoutPersistsAcrossRepositoryInstances() =
        runBlocking {
            var now = 1_000L
            val firstRepository = authRepository { now }
            firstRepository.createFirstAdmin("Admin", "admin", "correct-password".toCharArray())
            firstRepository.logout()

            repeat(AuthPolicy.MAX_FAILED_LOGIN_ATTEMPTS) {
                assertEquals(
                    LoginResult.InvalidCredentials,
                    firstRepository.login("admin", "wrong-password".toCharArray())
                )
            }
            assertEquals(
                LoginResult.LockedOut,
                firstRepository.login("admin", "correct-password".toCharArray())
            )

            val restartedRepository = authRepository { now }
            assertEquals(
                LoginResult.LockedOut,
                restartedRepository.login("admin", "correct-password".toCharArray())
            )

            now += AuthPolicy.LOGIN_LOCKOUT_MILLIS + 1L
            assertTrue(
                restartedRepository.login(
                    "admin",
                    "correct-password".toCharArray()
                ) is LoginResult.Success
            )
        }

    @Test
    fun authStateIsInitializedAndConfiguredWhenAdminExists() =
        runBlocking {
            seedAdmin()
            val state = authRepository { NOW }.observeAuthState().first()

            assertTrue(state.initialized)
            assertTrue(state.configured)
        }

    @Test
    fun loginKeepsCurrentUserAndAllowsSavingCategory() =
        runBlocking {
            val authRepository = authRepository { NOW }
            authRepository.createFirstAdmin("Admin", "admin", "correct-password".toCharArray())
            authRepository.logout()

            val loginResult = authRepository.login("admin", "correct-password".toCharArray())

            assertTrue(loginResult is LoginResult.Success)
            val actor = authRepository.currentUser.value
            assertNotNull(actor)

            CatalogRepository(context, database.orderCategoryDao())
                .saveCategory(
                    actor = actor!!,
                    input =
                        CategoryInput(
                            id = "saved-after-login",
                            name = "Notebook",
                            description = "Riparazioni notebook",
                            active = true,
                        ),
                ).getOrThrow()

            assertNotNull(database.orderCategoryDao().findById("saved-after-login"))
        }

    @Test
    fun authStateEmitsCurrentUserAfterSingleSuccessfulLogin() =
        runBlocking {
            val authRepository = authRepository { NOW }
            authRepository.createFirstAdmin("Admin", "admin", "correct-password".toCharArray())
            authRepository.logout()

            assertTrue(
                authRepository.login(
                    "admin",
                    "correct-password".toCharArray()
                ) is LoginResult.Success
            )

            val state = authRepository.observeAuthState().first { it.currentUser != null }
            assertEquals("admin", state.currentUser?.username)
        }

    @Test
    fun workflowRepositoryCreatesOrderAndTimelineEvent() =
        runBlocking {
            val actor = seedAdmin()
            seedCustomer()
            seedCategory()
            val repository = orderWorkflowRepository()

            val order =
                repository
                    .createOrder(
                        actor = actor,
                        input =
                            RepairOrderInput(
                                customerId = CUSTOMER_ID,
                                title = "Schermo rotto",
                                categoryId = CATEGORY_ID,
                                description = "Vetro crepato",
                            ),
                    ).getOrThrow()

            assertNotNull(database.repairOrderDao().findById(order.id))
            val events = database.timelineEventDao().observeForOrder(order.id).first()
            assertEquals(1, events.size)
            assertEquals(order.id, events.single().orderId)
        }

    @Test
    fun diagnosisWorkflowCoversRepairableRejectedAndNotRepairableBranches() =
        runBlocking {
            val actor = seedAdmin()
            seedCustomer()
            seedCategory()
            val workflow = orderWorkflowRepository()
            val templates = templateRepository()
            templates.ensureDefaults(actor.id)

            val acceptedOrder = createDiagnosisOrder(workflow, actor, "Riparabile")
            workflow
                .recordResult(
                    actor,
                    acceptedOrder.id,
                    DiagnosisResultInput(
                        "Display sostituibile",
                        DiagnosisOutcome.REPAIRABLE,
                        12_300L
                    ),
                ).getOrThrow()
            val acceptedResult = database.repairOrderDao().findById(acceptedOrder.id)
            assertEquals(DiagnosisOutcome.REPAIRABLE, acceptedResult?.diagnosisOutcome)
            assertEquals(12_300L, acceptedResult?.estimateAmountCents)
            val quote =
                templates.prepareMessage(actor, acceptedOrder.id, TemplateAnchor.WHATSAPP_QUOTE)
                    .getOrThrow()
            assertTrue(quote.body.contains("EUR 123,00"))
            templates.recordPrepared(actor, quote, quote.body).getOrThrow()
            workflow
                .recordFeedback(
                    actor,
                    acceptedOrder.id,
                    FeedbackInput(CustomerFeedbackType.ACCEPT, "Preventivo accettato"),
                ).getOrThrow()
            assertEquals(
                OrderStatus.REPAIRING,
                database.repairOrderDao().findById(acceptedOrder.id)?.status
            )
            assertEquals(
                CustomerFeedbackType.ACCEPT,
                database.messageLogDao().findLatestForOrder(acceptedOrder.id)?.actualReply
            )

            val rejectedOrder = createDiagnosisOrder(workflow, actor, "Rifiutata")
            workflow
                .recordResult(
                    actor,
                    rejectedOrder.id,
                    DiagnosisResultInput(
                        "Riparazione possibile",
                        DiagnosisOutcome.REPAIRABLE,
                        9_900L
                    ),
                ).getOrThrow()
            workflow
                .recordFeedback(
                    actor,
                    rejectedOrder.id,
                    FeedbackInput(CustomerFeedbackType.REJECT, "Preventivo non accettato"),
                ).getOrThrow()
            val rejected = database.repairOrderDao().findById(rejectedOrder.id)
            assertEquals(OrderStatus.CLOSED, rejected?.status)
            assertEquals(ClosureReason.CUSTOMER_DECLINED, rejected?.closureReason)

            val notRepairableOrder = createDiagnosisOrder(workflow, actor, "Non riparabile")
            workflow
                .recordResult(
                    actor,
                    notRepairableOrder.id,
                    DiagnosisResultInput(
                        "Scheda irreparabile",
                        DiagnosisOutcome.NOT_REPAIRABLE,
                        50_000L
                    ),
                ).getOrThrow()
            val notRepairable = database.repairOrderDao().findById(notRepairableOrder.id)
            assertEquals(DiagnosisOutcome.NOT_REPAIRABLE, notRepairable?.diagnosisOutcome)
            assertEquals(null, notRepairable?.estimateAmountCents)
            val message = templates.prepareMessage(
                actor,
                notRepairableOrder.id,
                TemplateAnchor.WHATSAPP_NOT_REPAIRABLE
            ).getOrThrow()
            assertTrue(message.body.contains("non riparabile"))
            assertFalse(message.body.contains("Motivo: ."))
            workflow
                .changeStatus(
                    actor,
                    notRepairableOrder.id,
                    OrderStatus.READY_FOR_PICKUP,
                    "Cliente avvisato",
                ).getOrThrow()
            workflow
                .changeStatus(
                    actor,
                    notRepairableOrder.id,
                    OrderStatus.CLOSED,
                    "Dispositivo ritirato",
                    ClosureReason.NOT_REPAIRABLE,
                ).getOrThrow()
            assertEquals(
                OrderStatus.CLOSED,
                database.repairOrderDao().findById(notRepairableOrder.id)?.status
            )
        }

    @Test
    fun diagnosisOutcomeTemplateVariableSavesAndRendersAllStates() =
        runBlocking {
            val actor = seedAdmin()
            seedCustomer()
            seedCategory()
            val workflow = orderWorkflowRepository()
            val templates = templateRepository()
            templates
                .saveTemplate(
                    actor = actor,
                    code = "diagnosis_outcome_test",
                    title = "Esito diagnosi",
                    body = "Esito: {{esito_diagnosi}}",
                    active = true,
                ).getOrThrow()

            val repairable =
                workflow
                    .createOrder(
                        actor,
                        RepairOrderInput(CUSTOMER_ID, "Riparabile", CATEGORY_ID),
                    ).getOrThrow()
            database.repairOrderDao()
                .upsert(repairable.copy(diagnosisOutcome = DiagnosisOutcome.REPAIRABLE))

            val notRepairable =
                workflow
                    .createOrder(
                        actor,
                        RepairOrderInput(CUSTOMER_ID, "Non riparabile", CATEGORY_ID),
                    ).getOrThrow()
            database.repairOrderDao()
                .upsert(notRepairable.copy(diagnosisOutcome = DiagnosisOutcome.NOT_REPAIRABLE))

            val missing =
                workflow
                    .createOrder(
                        actor,
                        RepairOrderInput(CUSTOMER_ID, "Non registrato", CATEGORY_ID),
                    ).getOrThrow()

            assertEquals(
                "Esito: Riparabile",
                templates.prepareMessage(actor, repairable.id, "diagnosis_outcome_test")
                    .getOrThrow().body,
            )
            assertEquals(
                "Esito: Non riparabile",
                templates.prepareMessage(actor, notRepairable.id, "diagnosis_outcome_test")
                    .getOrThrow().body,
            )
            assertEquals(
                "Esito: ",
                templates.prepareMessage(actor, missing.id, "diagnosis_outcome_test")
                    .getOrThrow().body,
            )
        }

    @Test
    fun customerUpdatePreservesExistingOrderRelationship() =
        runBlocking {
            val actor = seedAdmin()
            seedCustomer()
            seedCategory()
            val order =
                orderWorkflowRepository()
                    .createOrder(
                        actor = actor,
                        input =
                            RepairOrderInput(
                                customerId = CUSTOMER_ID,
                                title = "Schermo rotto",
                                categoryId = CATEGORY_ID,
                            ),
                    ).getOrThrow()
            val repository = CustomerRepository(database.customerDao(), clockMillis = { NOW + 1L })

            val updated =
                repository
                    .save(
                        actor,
                        com.onlytechtake.repairapp.domain.workspace.CustomerInput(
                            id = CUSTOMER_ID,
                            type = CustomerType.PRIVATE,
                            firstName = "Luigi",
                            lastName = "Verdi",
                            phone = "3331111111",
                            email = "luigi@example.test",
                        ),
                    ).getOrThrow()

            assertEquals(CUSTOMER_ID, updated.id)
            assertEquals(CUSTOMER_ID, database.repairOrderDao().findById(order.id)?.customerId)
            assertEquals("Luigi", database.customerDao().findById(CUSTOMER_ID)?.firstName)
        }

    @Test
    fun backupRepositoryExportsConfigurationAndRecordsAudit() =
        runBlocking {
            val actor = seedAdmin()
            seedCategory()
            database.messageTemplateDao().upsert(
                MessageTemplateEntity(
                    id = "template-1",
                    code = "status",
                    title = "Stato",
                    channel = MessageChannel.WHATSAPP,
                    body = "Commessa aggiornata",
                    active = true,
                    version = 1,
                    updatedByUserId = actor.id,
                    updatedAt = NOW,
                ),
            )
            database.appSettingDao().upsert(
                AppSettingEntity(
                    key = "message_mode",
                    value = "MANUAL_WHATSAPP",
                    updatedByUserId = actor.id,
                    updatedAt = NOW,
                ),
            )
            val output = File(context.cacheDir, "configuration-test.json")
            val repository = backupRepository()

            repository.exportConfiguration(actor, Uri.fromFile(output)).getOrThrow()

            val text = output.readText()
            assertTrue(text.contains("\"categories\""))
            assertTrue(text.contains("Smartphone"))
            assertEquals(
                1,
                database
                    .auditLogDao()
                    .observeRecent()
                    .first()
                    .size,
            )
        }

    @Test
    fun backupRepositoryExportsAllOrdersAsXlsxAndRecordsAudit() =
        runBlocking {
            if (BuildConfig.IS_DEMO_VERSION) return@runBlocking
            val actor = seedAdmin()
            seedCustomer()
            seedCategory()
            orderWorkflowRepository()
                .createOrder(
                    actor = actor,
                    input =
                        RepairOrderInput(
                            customerId = CUSTOMER_ID,
                            title = "Schermo & vetro",
                            categoryId = CATEGORY_ID,
                            description = "Display rotto",
                        ),
                ).getOrThrow()
            val output = File(context.cacheDir, "orders-export-test.xlsx")
            val repository =
                BackupRepository(
                    context = context,
                    categoryDao = database.orderCategoryDao(),
                    templateDao = database.messageTemplateDao(),
                    settingDao = database.appSettingDao(),
                    auditLogDao = database.auditLogDao(),
                    orderExportDao = database.orderExportDao(),
                    clockMillis = { NOW },
                )

            repository.exportOrdersXlsx(actor, Uri.fromFile(output)).getOrThrow()

            val entries = output.unzipTextEntries()
            assertTrue(entries.containsKey("[Content_Types].xml"))
            assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
            assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("202"))
            assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("Schermo &amp; vetro"))
            assertEquals(
                1,
                database
                    .auditLogDao()
                    .observeRecent()
                    .first()
                    .size,
            )

            output.delete()
            Unit
        }

    @Test
    fun backupRepositoryExportsCompleteZipWithMetadataDatabaseAndPrivateFiles() =
        runBlocking {
            if (BuildConfig.IS_DEMO_VERSION) return@runBlocking
            val databaseName = RepairAppDatabase.DATABASE_NAME
            context.deleteDatabase(databaseName)
            val key = DatabaseKeyManager(context).getDatabaseKey()
            val factory = SupportOpenHelperFactory(key.toByteArray())
            val fileBackedDatabase =
                Room
                    .databaseBuilder(context, RepairAppDatabase::class.java, databaseName)
                    .openHelperFactory(factory)
                    .allowMainThreadQueries()
                    .build()
            val output = File(context.cacheDir, "complete-backup-test.zip")
            try {
                val hash = PasswordHasher().hash("correct-password".toCharArray())
                val actor = AuthenticatedUser(ADMIN_ID, "Admin", "admin", UserRole.ADMIN)
                fileBackedDatabase.userDao().upsert(
                    UserEntity(
                        id = actor.id,
                        displayName = actor.displayName,
                        username = actor.username,
                        passwordHash = hash.hash,
                        passwordSalt = hash.salt,
                        role = UserRole.ADMIN,
                        active = true,
                        createdAt = NOW,
                        updatedAt = NOW,
                    ),
                )
                fileBackedDatabase.orderCategoryDao().upsert(
                    OrderCategoryEntity(
                        id = CATEGORY_ID,
                        parentId = null,
                        level = 0,
                        name = "Smartphone",
                        description = null,
                        active = true,
                    ),
                )
                fileBackedDatabase.appSettingDao().upsert(
                    AppSettingEntity(
                        key = "message_mode",
                        value = "MANUAL_WHATSAPP",
                        updatedByUserId = actor.id,
                        updatedAt = NOW,
                    ),
                )
                File(context.filesDir, "app_media/signatures/order-1/signature.png").also {
                    it.parentFile?.mkdirs()
                    it.writeText("signature")
                }
                File(context.filesDir, "app_media/order_media/order-1/photo.jpg").also {
                    it.parentFile?.mkdirs()
                    it.writeBytes(byteArrayOf(1, 2, 3, 4))
                }
                val repository =
                    BackupRepository(
                        context = context,
                        categoryDao = fileBackedDatabase.orderCategoryDao(),
                        templateDao = fileBackedDatabase.messageTemplateDao(),
                        settingDao = fileBackedDatabase.appSettingDao(),
                        auditLogDao = fileBackedDatabase.auditLogDao(),
                        clockMillis = { NOW },
                    )

                repository.exportCompleteBackup(actor, Uri.fromFile(output)).getOrThrow()

                val entries = output.unzipTextEntries()
                assertTrue(
                    entries.getValue("metadata/backup.json")
                        .contains("\"databaseVersion\": ${RepairAppDatabase.DATABASE_VERSION}")
                )
                assertTrue(entries.getValue("config/configuration.json").contains("Smartphone"))
                assertTrue(entries.containsKey("database/$databaseName"))
                assertTrue(
                    entries.getValue("files/app_media/signatures/order-1/signature.png")
                        .contains("signature")
                )
                assertTrue(entries.containsKey("files/app_media/order_media/order-1/photo.jpg"))
                assertEquals(
                    1,
                    fileBackedDatabase
                        .auditLogDao()
                        .observeRecent()
                        .first()
                        .size,
                )
            } finally {
                fileBackedDatabase.close()
                output.delete()
                context.deleteDatabase(databaseName)
            }

            Unit
        }

    @Test
    fun backupRepositoryRestoresCompleteZipReplacingDatabaseAndPrivateFiles() =
        runBlocking {
            if (BuildConfig.IS_DEMO_VERSION) return@runBlocking
            val databaseName = RepairAppDatabase.DATABASE_NAME
            context.deleteDatabase(databaseName)
            val key = DatabaseKeyManager(context).getDatabaseKey()
            val factory = SupportOpenHelperFactory(key.toByteArray())
            val fileBackedDatabase =
                Room
                    .databaseBuilder(context, RepairAppDatabase::class.java, databaseName)
                    .openHelperFactory(factory)
                    .allowMainThreadQueries()
                    .build()
            val output = File(context.cacheDir, "complete-restore-test.zip")
            val sessionStore = InMemorySessionStore()
            try {
                val hash = PasswordHasher().hash("correct-password".toCharArray())
                val actor = AuthenticatedUser(ADMIN_ID, "Admin", "admin", UserRole.ADMIN)
                sessionStore.setCurrentUser(actor, NOW)
                fileBackedDatabase.userDao().upsert(
                    UserEntity(
                        id = actor.id,
                        displayName = actor.displayName,
                        username = actor.username,
                        passwordHash = hash.hash,
                        passwordSalt = hash.salt,
                        role = UserRole.ADMIN,
                        active = true,
                        createdAt = NOW,
                        updatedAt = NOW,
                    ),
                )
                fileBackedDatabase.orderCategoryDao().upsert(
                    OrderCategoryEntity(
                        id = CATEGORY_ID,
                        parentId = null,
                        level = 0,
                        name = "Smartphone",
                        description = null,
                        active = true,
                    ),
                )
                File(context.filesDir, "app_media/order_media/order-1/photo.jpg").also {
                    it.parentFile?.mkdirs()
                    it.writeBytes(byteArrayOf(1, 2, 3, 4))
                }
                val repository =
                    BackupRepository(
                        context = context,
                        categoryDao = fileBackedDatabase.orderCategoryDao(),
                        templateDao = fileBackedDatabase.messageTemplateDao(),
                        settingDao = fileBackedDatabase.appSettingDao(),
                        auditLogDao = fileBackedDatabase.auditLogDao(),
                        database = fileBackedDatabase,
                        authPreferences = authPreferences,
                        sessionStore = sessionStore,
                        clockMillis = { NOW },
                    )

                repository.exportCompleteBackup(actor, Uri.fromFile(output)).getOrThrow()
                fileBackedDatabase.orderCategoryDao().upsert(
                    OrderCategoryEntity(
                        id = CATEGORY_ID,
                        parentId = null,
                        level = 0,
                        name = "Categoria sovrascritta",
                        description = null,
                        active = true,
                    ),
                )
                File(
                    context.filesDir,
                    "app_media/order_media/order-1/photo.jpg"
                ).writeText("changed")

                repository.restoreCompleteBackup(actor, Uri.fromFile(output)).getOrThrow()

                val restoredDatabase =
                    Room
                        .databaseBuilder(context, RepairAppDatabase::class.java, databaseName)
                        .openHelperFactory(factory)
                        .allowMainThreadQueries()
                        .build()
                try {
                    assertEquals(
                        "Smartphone",
                        restoredDatabase.orderCategoryDao().findById(CATEGORY_ID)?.name
                    )
                    assertTrue(
                        File(
                            context.filesDir,
                            "app_media/order_media/order-1/photo.jpg"
                        ).readBytes().contentEquals(byteArrayOf(1, 2, 3, 4))
                    )
                    assertEquals(null, sessionStore.currentUser.value)
                } finally {
                    restoredDatabase.close()
                }
            } finally {
                runCatching { fileBackedDatabase.close() }
                output.delete()
                context.deleteDatabase(databaseName)
                clearAppFiles()
            }

            Unit
        }

    @Test
    fun dataResetClearsDatabasePreferencesSessionAndPrivateFiles() =
        runBlocking {
            val actor = seedAdmin()
            authPreferences.markConfigured()
            val sessionStore = InMemorySessionStore().also { it.setCurrentUser(actor, NOW) }
            val orderFile =
                File(context.filesDir, "app_media/documents/order-1/file.txt").also {
                    it.parentFile?.mkdirs()
                    it.writeText("private")
                }
            val mediaProvider = MediaStorageProvider(context)
            val repository =
                DataResetRepository(
                    context = context,
                    database = database,
                    auditLogDao = database.auditLogDao(),
                    authPreferences = authPreferences,
                    sessionStore = sessionStore,
                    fileCleanupService = OrderFileCleanupService(context, mediaProvider),
                    mediaStorageProvider = mediaProvider,
                )

            repository.resetAll(actor, "RESET").getOrThrow()

            assertEquals(null, database.userDao().findById(actor.id))
            assertFalse(orderFile.exists())
            assertEquals(null, sessionStore.currentUser.value)
        }

    @Test
    fun dataResetClearsFileBackedDatabaseWithoutLocking() =
        runBlocking {
            val databaseName = "reset-file-backed.db"
            context.deleteDatabase(databaseName)
            val key = DatabaseKeyManager(context).getDatabaseKey()
            val factory = SupportOpenHelperFactory(key.toByteArray())
            val fileBackedDatabase =
                Room
                    .databaseBuilder(context, RepairAppDatabase::class.java, databaseName)
                    .openHelperFactory(factory)
                    .allowMainThreadQueries()
                    .build()
            val actor = AuthenticatedUser(ADMIN_ID, "Admin", "admin", UserRole.ADMIN)
            val sessionStore = InMemorySessionStore().also { it.setCurrentUser(actor, NOW) }
            val authPreferences = AuthPreferences(context)
            try {
                val hash = PasswordHasher().hash("correct-password".toCharArray())
                fileBackedDatabase.userDao().upsert(
                    UserEntity(
                        id = ADMIN_ID,
                        displayName = "Admin",
                        username = "admin",
                        passwordHash = hash.hash,
                        passwordSalt = hash.salt,
                        role = UserRole.ADMIN,
                        active = true,
                        createdAt = NOW,
                        updatedAt = NOW,
                    ),
                )
                fileBackedDatabase.customerDao().upsert(
                    CustomerEntity(
                        id = CUSTOMER_ID,
                        type = CustomerType.PRIVATE,
                        firstName = "Mario",
                        lastName = "Rossi",
                        companyName = null,
                        vatNumber = null,
                        taxCode = null,
                        phone = "3330000000",
                        email = null,
                        address = null,
                        notes = null,
                        createdAt = NOW,
                        updatedAt = NOW,
                    ),
                )
                fileBackedDatabase.orderCategoryDao().upsert(
                    OrderCategoryEntity(
                        id = CATEGORY_ID,
                        parentId = null,
                        level = 0,
                        name = "Smartphone",
                        description = null,
                        active = true,
                    ),
                )
                fileBackedDatabase.repairOrderDao().upsert(
                    RepairOrderEntity(
                        id = "order-1",
                        orderNumber = "2026-0001",
                        customerId = CUSTOMER_ID,
                        title = "Schermo rotto",
                        categoryId = CATEGORY_ID,
                        status = OrderStatus.OPEN,
                        priority = OrderPriority.NORMAL,
                        assignedToUserId = ADMIN_ID,
                        createdByUserId = ADMIN_ID,
                        description = null,
                        internalNotes = null,
                        estimateAmountCents = null,
                        finalAmountCents = null,
                        createdAt = NOW,
                        updatedAt = NOW,
                        closedAt = null,
                        closureReason = null,
                    ),
                )
                val orderFile =
                    File(context.filesDir, "app_media/documents/order-1/file.txt").also {
                        it.parentFile?.mkdirs()
                        it.writeText("private")
                    }
                val mediaProvider = MediaStorageProvider(context)
                val repository =
                    DataResetRepository(
                        context = context,
                        database = fileBackedDatabase,
                        auditLogDao = fileBackedDatabase.auditLogDao(),
                        authPreferences = authPreferences,
                        sessionStore = sessionStore,
                        fileCleanupService = OrderFileCleanupService(context, mediaProvider),
                        mediaStorageProvider = mediaProvider,
                    )

                repository.resetAll(actor, "RESET").getOrThrow()

                assertEquals(null, fileBackedDatabase.userDao().findById(actor.id))
                assertEquals(null, fileBackedDatabase.repairOrderDao().findById("order-1"))
                assertFalse(orderFile.exists())
                assertEquals(null, sessionStore.currentUser.value)
            } finally {
                authPreferences.clear()
                fileBackedDatabase.close()
                context.deleteDatabase(databaseName)
            }
        }

    @Test
    fun intakeUseCaseDeletesSignatureAndAttachedMediaWhenOuterTransactionFails() =
        runBlocking {
            val actor = seedAdmin()
            seedCustomer()
            seedCategory()
            val mediaRepository = orderMediaRepository()
            val signatureRepository = signatureRepository()
            val useCase =
                CreateIntakeOrderUseCase(
                    context = context,
                    customerRepository = CustomerRepository(database.customerDao()) { NOW },
                    orderRepository = orderWorkflowRepository(),
                    orderMediaRepository = mediaRepository,
                    signatureRepository = signatureRepository,
                    runAtomically = { block ->
                        block()
                        error("Commit fallito")
                    },
                )
            val stagedFile =
                File(context.filesDir, "app_media/intake_media/session/photo.jpg").also {
                    it.parentFile?.mkdirs()
                    it.writeBytes(byteArrayOf(1, 2, 3, 4))
                }
            val staged =
                StagedOrderMedia(
                    localPath = stagedFile.absolutePath,
                    fileName = stagedFile.name,
                    mimeType = "image/jpeg",
                    mediaType = "PHOTO",
                    sizeBytes = stagedFile.length(),
                    width = 1,
                    height = 1,
                )
            val signature = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

            val result =
                useCase.execute(
                    actor = actor,
                    input =
                        IntakeOrderInput(
                            existingCustomerId = CUSTOMER_ID,
                            problemSummary = "Schermo rotto",
                            categoryId = CATEGORY_ID,
                        ),
                    stagedMedia = listOf(staged),
                    signerName = "Mario Rossi",
                    privacyAccepted = true,
                    termsAccepted = true,
                    signatureBitmap = signature,
                )

            assertTrue(result.isFailure)
            assertFalse(File(context.filesDir, "app_media/order_media").walkFiles().any())
            assertFalse(File(context.filesDir, "app_media/signatures").walkFiles().any())
        }

    @Test
    fun deleteCategoryRemovesBranchAndFailsIfUsed() =
        runBlocking {
            val actor = seedAdmin()
            val dao = database.orderCategoryDao()
            val repository = CatalogRepository(context, dao)

            dao.upsert(OrderCategoryEntity("root", null, 0, "Root", null, true))
            dao.upsert(OrderCategoryEntity("child", "root", 1, "Child", null, true))

            // Delete fails if used
            seedCustomer()
            orderWorkflowRepository()
                .createOrder(
                    actor = actor,
                    input =
                        RepairOrderInput(
                            customerId = CUSTOMER_ID,
                            title = "Order",
                            categoryId = "child",
                        ),
                ).getOrThrow()

            val deleteResult = repository.deleteCategory(actor, "root")
            assertTrue(deleteResult.isFailure)
            assertTrue(deleteResult.exceptionOrNull()?.message?.contains("usata") == true)

            // Delete succeeds if NOT used
            database.repairOrderDao().deleteByIds(
                database
                    .repairOrderDao()
                    .observeRecentActiveOrders()
                    .first()
                    .map { it.id },
            )

            repository.deleteCategory(actor, "root").getOrThrow()
            assertEquals(0, dao.findAll().size)
        }

    private fun authRepository(clockMillis: () -> Long): DefaultAuthRepository =
        DefaultAuthRepository(
            userDao = database.userDao(),
            auditLogDao = database.auditLogDao(),
            loginAttemptDao = database.loginAttemptDao(),
            appSettingDao = database.appSettingDao(),
            authPreferences = authPreferences,
            sessionStore = InMemorySessionStore(),
            passwordHasher = PasswordHasher(),
            clockMillis = clockMillis,
        )

    private fun orderWorkflowRepository(): OrderWorkflowRepository =
        OrderWorkflowRepository(
            context = context,
            repairOrderDao = database.repairOrderDao(),
            customerDao = database.customerDao(),
            categoryDao = database.orderCategoryDao(),
            userDao = database.userDao(),
            timelineEventDao = database.timelineEventDao(),
            orderMediaDao = database.orderMediaDao(),
            signatureDao = database.signatureDao(),
            messageLogDao = database.messageLogDao(),
            auditLogDao = database.auditLogDao(),
            database = database,
            clockMillis = { NOW },
        )

    private fun orderMediaRepository(): OrderMediaRepository {
        val mediaProvider = MediaStorageProvider(context)
        return OrderMediaRepository(
            context = context,
            repairOrderDao = database.repairOrderDao(),
            orderMediaDao = database.orderMediaDao(),
            timelineEventDao = database.timelineEventDao(),
            fileCleanupService = OrderFileCleanupService(context, mediaProvider),
            mediaStorageProvider = mediaProvider,
            database = database,
            clockMillis = { NOW },
        )
    }

    private fun templateRepository(): TemplateRepository =
        TemplateRepository(
            context = context,
            templateDao = database.messageTemplateDao(),
            orderDao = database.repairOrderDao(),
            customerDao = database.customerDao(),
            categoryDao = database.orderCategoryDao(),
            settingDao = database.appSettingDao(),
            messageLogDao = database.messageLogDao(),
            timelineEventDao = database.timelineEventDao(),
            auditLogDao = database.auditLogDao(),
            clockMillis = { NOW },
        )

    private fun signatureRepository(): SignatureRepository {
        val mediaProvider = MediaStorageProvider(context)
        return SignatureRepository(
            context = context,
            repairOrderDao = database.repairOrderDao(),
            signatureDao = database.signatureDao(),
            templateDao = database.messageTemplateDao(),
            settingDao = database.appSettingDao(),
            timelineEventDao = database.timelineEventDao(),
            fileCleanupService = OrderFileCleanupService(context, mediaProvider),
            mediaStorageProvider = mediaProvider,
            clockMillis = { NOW },
        )
    }

    private fun backupRepository(): BackupRepository =
        BackupRepository(
            context = context,
            categoryDao = database.orderCategoryDao(),
            templateDao = database.messageTemplateDao(),
            settingDao = database.appSettingDao(),
            auditLogDao = database.auditLogDao(),
            mediaStorageProvider = MediaStorageProvider(context),
            clockMillis = { NOW },
        )

    private suspend fun seedAdmin(): AuthenticatedUser {
        val hash = PasswordHasher().hash("correct-password".toCharArray())
        database.userDao().upsert(
            UserEntity(
                id = ADMIN_ID,
                displayName = "Admin",
                username = "admin",
                passwordHash = hash.hash,
                passwordSalt = hash.salt,
                role = UserRole.ADMIN,
                active = true,
                createdAt = NOW,
                updatedAt = NOW,
            ),
        )
        return AuthenticatedUser(ADMIN_ID, "Admin", "admin", UserRole.ADMIN)
    }

    private suspend fun seedCustomer() {
        database.customerDao().upsert(
            CustomerEntity(
                id = CUSTOMER_ID,
                type = CustomerType.PRIVATE,
                firstName = "Mario",
                lastName = "Rossi",
                companyName = null,
                vatNumber = null,
                taxCode = null,
                phone = "3330000000",
                email = null,
                address = null,
                notes = null,
                createdAt = NOW,
                updatedAt = NOW,
            ),
        )
    }

    private suspend fun seedCategory() {
        database.orderCategoryDao().upsert(
            OrderCategoryEntity(
                id = CATEGORY_ID,
                parentId = null,
                level = 0,
                name = "Smartphone",
                description = null,
                active = true,
            ),
        )
    }

    private suspend fun createDiagnosisOrder(
        repository: OrderWorkflowRepository,
        actor: AuthenticatedUser,
        title: String,
    ): RepairOrderEntity {
        val order =
            repository
                .createOrder(
                    actor,
                    RepairOrderInput(
                        customerId = CUSTOMER_ID,
                        title = title,
                        categoryId = CATEGORY_ID,
                    ),
                ).getOrThrow()
        repository.changeStatus(actor, order.id, OrderStatus.IN_PROGRESS).getOrThrow()
        repository.changeStatus(actor, order.id, OrderStatus.DIAGNOSIS).getOrThrow()
        return order
    }

    private fun clearAppFiles() {
        listOf("app_media", "orders", "order_media", "intake_media", "signatures").forEach { name ->
            File(context.filesDir, name).deleteRecursively()
        }
    }

    private fun File.walkFiles(): Sequence<File> =
        if (exists()) walkTopDown().filter { it.isFile } else emptySequence()

    private fun File.unzipTextEntries(): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }
        return entries
    }

    private companion object {
        const val NOW = 1_717_171_717_000L
        const val ADMIN_ID = "admin-1"
        const val CUSTOMER_ID = "customer-1"
        const val CATEGORY_ID = "category-1"
    }
}
