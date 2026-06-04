package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthPolicy
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.auth.LoginResult
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.domain.workspace.RepairOrderInput
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryPathsInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: RepairAppDatabase
    private lateinit var authPreferences: AuthPreferences

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, RepairAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        authPreferences = AuthPreferences(context)
        authPreferences.clear()
        clearAppFiles()
    }

    @After
    fun tearDown() = runBlocking {
        authPreferences.clear()
        database.close()
        clearAppFiles()
    }

    @Test
    fun authLockoutPersistsAcrossRepositoryInstances() = runBlocking {
        var now = 1_000L
        val firstRepository = authRepository { now }
        firstRepository.createFirstAdmin("Admin", "admin", "correct-password".toCharArray())
        firstRepository.logout()

        repeat(AuthPolicy.MAX_FAILED_LOGIN_ATTEMPTS) {
            assertEquals(LoginResult.InvalidCredentials, firstRepository.login("admin", "wrong-password".toCharArray()))
        }
        assertEquals(LoginResult.LockedOut, firstRepository.login("admin", "correct-password".toCharArray()))

        val restartedRepository = authRepository { now }
        assertEquals(LoginResult.LockedOut, restartedRepository.login("admin", "correct-password".toCharArray()))

        now += AuthPolicy.LOGIN_LOCKOUT_MILLIS + 1L
        assertTrue(restartedRepository.login("admin", "correct-password".toCharArray()) is LoginResult.Success)
    }

    @Test
    fun workflowRepositoryCreatesOrderAndTimelineEvent() = runBlocking {
        val actor = seedAdmin()
        seedCustomer()
        seedCategory()
        val repository = orderWorkflowRepository()

        val order = repository.createOrder(
            actor = actor,
            input = RepairOrderInput(
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
    fun backupRepositoryExportsConfigurationAndRecordsAudit() = runBlocking {
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
        assertEquals(1, database.auditLogDao().observeRecent().first().size)
    }

    @Test
    fun dataResetClearsDatabasePreferencesSessionAndPrivateFiles() = runBlocking {
        val actor = seedAdmin()
        authPreferences.markConfigured()
        val sessionStore = InMemorySessionStore().also { it.setCurrentUser(actor, NOW) }
        val orderFile = File(context.filesDir, "orders/order-1/file.txt").also {
            it.parentFile?.mkdirs()
            it.writeText("private")
        }
        val repository = DataResetRepository(
            context = context,
            database = database,
            auditLogDao = database.auditLogDao(),
            authPreferences = authPreferences,
            sessionStore = sessionStore,
        )

        repository.resetAll(actor, "RESET").getOrThrow()

        assertEquals(null, database.userDao().findById(actor.id))
        assertFalse(orderFile.exists())
        assertEquals(null, sessionStore.currentUser.value)
    }

    @Test
    fun intakeUseCaseDeletesSignatureAndAttachedMediaWhenOuterTransactionFails() = runBlocking {
        val actor = seedAdmin()
        seedCustomer()
        seedCategory()
        val mediaRepository = orderMediaRepository()
        val signatureRepository = signatureRepository()
        val useCase = CreateIntakeOrderUseCase(
            customerRepository = CustomerRepository(database.customerDao()) { NOW },
            orderRepository = orderWorkflowRepository(),
            orderMediaRepository = mediaRepository,
            signatureRepository = signatureRepository,
            runAtomically = { block ->
                block()
                error("Commit fallito")
            },
        )
        val stagedFile = File(context.filesDir, "intake_media/session/photo.jpg").also {
            it.parentFile?.mkdirs()
            it.writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val staged = StagedOrderMedia(
            localPath = stagedFile.absolutePath,
            fileName = stagedFile.name,
            mimeType = "image/jpeg",
            mediaType = "PHOTO",
            sizeBytes = stagedFile.length(),
            width = 1,
            height = 1,
        )
        val signature = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val result = useCase.execute(
            actor = actor,
            input = IntakeOrderInput(
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
        assertFalse(File(context.filesDir, "order_media").walkFiles().any())
        assertFalse(File(context.filesDir, "orders").walkFiles().any())
    }

    private fun authRepository(clockMillis: () -> Long): DefaultAuthRepository =
        DefaultAuthRepository(
            userDao = database.userDao(),
            auditLogDao = database.auditLogDao(),
            authPreferences = authPreferences,
            sessionStore = InMemorySessionStore(),
            passwordHasher = PasswordHasher(),
            clockMillis = clockMillis,
        )

    private fun orderWorkflowRepository(): OrderWorkflowRepository =
        OrderWorkflowRepository(
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

    private fun orderMediaRepository(): OrderMediaRepository =
        OrderMediaRepository(
            context = context,
            repairOrderDao = database.repairOrderDao(),
            orderMediaDao = database.orderMediaDao(),
            timelineEventDao = database.timelineEventDao(),
            database = database,
            clockMillis = { NOW },
        )

    private fun signatureRepository(): SignatureRepository =
        SignatureRepository(
            context = context,
            repairOrderDao = database.repairOrderDao(),
            signatureDao = database.signatureDao(),
            templateDao = database.messageTemplateDao(),
            timelineEventDao = database.timelineEventDao(),
            clockMillis = { NOW },
        )

    private fun backupRepository(): BackupRepository =
        BackupRepository(
            context = context,
            categoryDao = database.orderCategoryDao(),
            templateDao = database.messageTemplateDao(),
            settingDao = database.appSettingDao(),
            auditLogDao = database.auditLogDao(),
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
                name = "Smartphone",
                description = null,
                active = true,
                sortOrder = 0,
            ),
        )
    }

    private fun clearAppFiles() {
        listOf("orders", "order_media", "intake_media", "signatures").forEach { name ->
            File(context.filesDir, name).deleteRecursively()
        }
    }

    private fun File.walkFiles(): Sequence<File> =
        if (exists()) walkTopDown().filter { it.isFile } else emptySequence()

    private companion object {
        const val NOW = 1_717_171_717_000L
        const val ADMIN_ID = "admin-1"
        const val CUSTOMER_ID = "customer-1"
        const val CATEGORY_ID = "category-1"
    }
}
