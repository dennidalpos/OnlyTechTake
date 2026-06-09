package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
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
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.features.workspace.OrderStatusAction
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OperatorIntakeDemoScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesOperatorIntakeDemo() {
        runBlocking { seedDemoProfile() }
        val outputDir = prepareScreenshotDirectory()

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForTag(RepairAppTestTags.AuthUsernameField)
            capture(outputDir, "01-login.png")

            loginOperatorDemo()
            capture(outputDir, "02-home.png")

            clickDashboardTab("NEW_ORDER")
            clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            typeTag(RepairAppTestTags.NewOrderFirstNameField, "Giulia", scroll = true)
            typeTag(RepairAppTestTags.NewOrderLastNameField, "Bianchi", scroll = true)
            typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 333 010 2020", scroll = true)
            typeTag(RepairAppTestTags.NewOrderEmailField, "giulia.bianchi@example.invalid", scroll = true)
            capture(outputDir, "03-new-customer.png")

            clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            waitForText("Cliente selezionato")
            clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)
            clickTag(RepairAppTestTags.categoryButton(DEMO_CATEGORY_ID), scroll = true)
            clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)
            typeTag(RepairAppTestTags.NewOrderProblemSummaryField, "Notebook non si accende", scroll = true)
            typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Il cliente segnala spegnimento improvviso dopo caduta accidentale.",
                scroll = true,
            )
            capture(outputDir, "04-category-problem.png")

            clickDashboardTab("ORDERS")
            clickTag(RepairAppTestTags.orderCard(DEMO_ORDER_ID), scroll = true)
            waitForText("Stato: Aperta")
            capture(outputDir, "05-order-created.png")

            applySimpleStatus(OrderStatusAction.START_PROGRESS, "Stato: In lavorazione")
            capture(outputDir, "06-in-progress.png")

            applySimpleStatus(OrderStatusAction.START_DIAGNOSIS, "Stato: In diagnosi")
            applyResultStatus()
            capture(outputDir, "07-diagnosis-result.png")

            acceptQuote()
            capture(outputDir, "08-repairing.png")

            applySimpleStatus(OrderStatusAction.READY_FOR_PICKUP, "Stato: Pronta al ritiro")
            capture(outputDir, "09-ready-for-pickup.png")

            closeOrder()
            clickTag(RepairAppTestTags.orderCard(DEMO_ORDER_ID), scroll = true)
            waitForText("Stato: Chiusa")
            capture(outputDir, "10-closed-timeline.png")
        } finally {
            scenario.close()
        }
    }

    private suspend fun seedDemoProfile() {
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
                username = "admin.demo",
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
                active = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.orderCategoryDao().upsert(
            OrderCategoryEntity(
                id = DEMO_CATEGORY_ID,
                name = "Notebook",
                description = "Diagnosi e riparazioni notebook e ultrabook.",
                active = true,
                sortOrder = 1,
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
                notes = "Cliente demo per video promozionale.",
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
                deletedAt = null,
                deletedByUserId = null,
                deleteReason = null,
            ),
        )

        val mediaPath = writeDemoBitmap("demo-oggetto.png", "Foto oggetto", Color.rgb(229, 241, 255))
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

        val signaturePath = writeDemoBitmap("demo-firma.png", "Firma cliente", Color.WHITE)
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
                    body = "Ciao {{customerName}}, aggiornamento commessa {{orderNumber}}: $title.",
                    active = true,
                    version = 1,
                    updatedByUserId = DEMO_ADMIN_ID,
                    updatedAt = now,
                ),
            )
        }
        AuthPreferences(context).markConfigured()
    }

    private fun applySimpleStatus(action: OrderStatusAction, expectedText: String) {
        clickTag(RepairAppTestTags.statusAction(action.name), scroll = true)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForText(expectedText)
    }

    private fun applyResultStatus() {
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name), scroll = true)
        typeTag(RepairAppTestTags.StatusResultSummaryField, "Connettore di alimentazione danneggiato.", scroll = false)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForText("Stato: Risultato")
    }

    private fun acceptQuote() {
        composeRule.onNodeWithText("Preventivo accettato").performScrollTo().performClick()
        composeRule.onNodeWithText("Conferma esito preventivo").performScrollTo().performClick()
        waitForText("Stato: In riparazione")
    }

    private fun closeOrder() {
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        typeTag(RepairAppTestTags.StatusNoteField, "Riparazione completata e consegnata al cliente.")
        clickTag(RepairAppTestTags.closureReason("COMPLETED"), scroll = true)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForText("Commesse")
    }

    private fun clickDashboardTab(destination: String) {
        val tabTag = RepairAppTestTags.dashboardTab(destination)
        repeat(DASHBOARD_TAB_SWIPE_ATTEMPTS) {
            if (isTagDisplayed(tabTag)) {
                clickTag(tabTag)
                return
            }
            composeRule.onNodeWithTag(RepairAppTestTags.DashboardTabs)
                .performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
            pauseForDemo()
        }
        composeRule.onNodeWithTag(tabTag).performClick()
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun loginOperatorDemo() {
        repeat(LOGIN_ATTEMPTS) { attempt ->
            waitForTag(RepairAppTestTags.AuthUsernameField)
            typeTag(RepairAppTestTags.AuthUsernameField, DEMO_OPERATOR_USERNAME)
            pauseForDemo()
            typeTag(RepairAppTestTags.AuthPasswordField, DEMO_PASSWORD)
            pauseForDemo()
            clickTag(RepairAppTestTags.AuthSubmitButton)

            if (waitForTagIfPresent(RepairAppTestTags.dashboardTab("HOME"), LOGIN_ATTEMPT_TIMEOUT_MILLIS)) {
                return
            }

            if (attempt == LOGIN_ATTEMPTS - 1 || !isTagPresent(RepairAppTestTags.AuthUsernameField)) {
                waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            }
            pauseForDemo()
        }
    }

    private fun typeTag(tag: String, text: String, scroll: Boolean = false) {
        waitForTagExists(tag)
        val node = composeRule.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.assertIsDisplayed()
        node.performClick()
        node.performTextReplacement(text)
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun clickTag(tag: String, scroll: Boolean = false) {
        waitForTagExists(tag)
        val node = composeRule.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.assertIsDisplayed()
        node.performClick()
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun waitForTag(tag: String) {
        waitForTagExists(tag)
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun waitForTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTagIfPresent(tag: String, timeoutMillis: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        do {
            if (isTagPresent(tag)) {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                return true
            }
            Thread.sleep(TAG_POLL_MILLIS)
        } while (System.currentTimeMillis() < deadline)
        return false
    }

    private fun isTagPresent(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun isTagDisplayed(tag: String): Boolean =
        runCatching {
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }.isSuccess

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(outputDir: File, fileName: String) {
        composeRule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        pauseForScreenshot()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        FileOutputStream(File(outputDir, fileName)).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
    }

    private fun pauseForDemo() {
        Thread.sleep(ACTION_PAUSE_MILLIS)
    }

    private fun pauseForScreenshot() {
        Thread.sleep(SCREENSHOT_PAUSE_MILLIS)
    }

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), SCREENSHOT_DIR_NAME)
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }

    private fun writeDemoBitmap(fileName: String, label: String, backgroundColor: Int): File {
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

    private companion object {
        const val DEMO_PASSWORD = "DemoPass123"
        const val DEMO_OPERATOR_USERNAME = "operatore.demo"
        const val DEMO_ADMIN_ID = "demo-admin"
        const val DEMO_OPERATOR_ID = "demo-operator"
        const val DEMO_CATEGORY_ID = "demo-category-notebook"
        const val DEMO_CUSTOMER_ID = "demo-customer"
        const val DEMO_ORDER_ID = "demo-order-operator-intake"
        const val SCREENSHOT_DIR_NAME = "demo-screenshots"
        const val WAIT_TIMEOUT_MILLIS = 30_000L
        const val LOGIN_ATTEMPTS = 2
        const val LOGIN_ATTEMPT_TIMEOUT_MILLIS = 20_000L
        const val DASHBOARD_TAB_SWIPE_ATTEMPTS = 4
        const val TAG_POLL_MILLIS = 250L
        const val ACTION_PAUSE_MILLIS = 1_400L
        const val SCREENSHOT_PAUSE_MILLIS = 2_000L
    }
}
