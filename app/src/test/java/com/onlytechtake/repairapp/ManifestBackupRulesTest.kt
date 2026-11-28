package com.onlytechtake.repairapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
        val dataExtractionRules =
            projectFile("src/main/res/xml/data_extraction_rules.xml").readText()
        val fullBackupContent = projectFile("src/main/res/xml/backup_rules.xml").readText()

        listOf("file", "database", "sharedpref").forEach { domain ->
            val pattern = """domain="$domain"\s+path="\."""".toRegex()
            assertTrue(
                "Missing exclude for $domain in data_extraction_rules",
                pattern.containsMatchIn(dataExtractionRules),
            )
            assertTrue(
                "Missing exclude for $domain in backup_rules",
                pattern.containsMatchIn(fullBackupContent),
            )
        }
    }

    private fun projectFile(path: String): File =
        listOf(
            File(path),
            File("app", path),
        ).firstOrNull(File::exists) ?: error("Missing project file: $path")
}
