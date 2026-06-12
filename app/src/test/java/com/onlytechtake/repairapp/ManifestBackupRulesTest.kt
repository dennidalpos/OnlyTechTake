package com.onlytechtake.repairapp

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestBackupRulesTest {
    @Test
    fun manifestKeepsCloudBackupDisabledAndMarksFragileUserData() {
        val manifest = projectFile("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android:allowBackup="false""""))
        assertTrue(manifest.contains("""android:hasFragileUserData="true""""))
        assertTrue(manifest.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""))
        assertTrue(manifest.contains("""android:fullBackupContent="@xml/backup_rules""""))
    }

    @Test
    fun backupRulesExcludeFilesDatabasesAndSharedPreferences() {
        val dataExtractionRules = projectFile("src/main/res/xml/data_extraction_rules.xml").readText()
        val fullBackupContent = projectFile("src/main/res/xml/backup_rules.xml").readText()

        listOf("file", "database", "sharedpref").forEach { domain ->
            assertTrue(dataExtractionRules.contains("""<exclude domain="$domain" path="." />"""))
            assertTrue(fullBackupContent.contains("""<exclude domain="$domain" path="." />"""))
        }
    }

    private fun projectFile(path: String): File =
        listOf(
            File(path),
            File("app", path),
        ).firstOrNull(File::exists) ?: error("Missing project file: $path")
}
