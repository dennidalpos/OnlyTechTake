package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkflowOperationalTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun flow0_Setup() {
        runBlocking {
            RepairAppDatabase.closeInstance()
            context.deleteDatabase(RepairAppDatabase.DATABASE_NAME)
            AuthPreferences(context).clear()
            OwnerPreferences(context).updateOwnerInfo(OwnerInfo())
        }
        val outputDir = prepareDirectory("workflow-setup")
        val robot = OperationalRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.setupWizardFlow(outputDir)
        }
    }

    @Test
    fun flow1_Intake() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareDirectory("workflow-intake")
        val robot = OperationalRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            robot.intakeFlow(outputDir)
        }
    }

    @Test
    fun flow2_Management() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareDirectory("workflow-management")
        val robot = OperationalRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            robot.managementFlow(outputDir)
        }
    }

    @Test
    fun flow3_AdminOperators() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareDirectory("workflow-admin-operators")
        val robot = OperationalRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.adminOperatorsFlow(outputDir)
        }
    }

    @Test
    fun flow4_AdminTemplates() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareDirectory("workflow-admin-templates")
        val robot = OperationalRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.adminTemplatesFlow(outputDir)
        }
    }

    @Test
    fun flow5_Backup() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareDirectory("workflow-backup")
        val robot = OperationalRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.backupFlow(outputDir)
        }
    }

    private fun prepareDirectory(name: String): File {
        val root = File(requireNotNull(context.getExternalFilesDir(null)), "workflows")
        val directory = File(root, name)
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}
