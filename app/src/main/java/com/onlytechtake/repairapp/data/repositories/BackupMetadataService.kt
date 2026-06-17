package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import kotlinx.coroutines.flow.first

internal class BackupMetadataService(
    private val categoryDao: OrderCategoryDao,
    private val templateDao: MessageTemplateDao,
    private val settingDao: AppSettingDao,
    private val clockMillis: () -> Long,
) {
    suspend fun buildConfigurationJson(): String {
        val categories = categoryDao.observeAll().first()
        val templates = templateDao.observeAll().first()
        val settings = settingDao.observeAll().first()
        return buildString {
            appendLine("{")
            appendLine("  \"exportedAt\": ${jsonString(formatTime(clockMillis()))},")
            appendLine("  \"categories\": [")
            appendLine(
                categories.joinToString(",\n") { category ->
                    "    {\"id\": ${jsonString(category.id)}, \"parentId\": ${jsonString(category.parentId)}, \"level\": ${category.level}, \"name\": ${jsonString(category.name)}, \"active\": ${category.active}}"
                },
            )
            appendLine("  ],")
            appendLine("  \"templates\": [")
            appendLine(
                templates.joinToString(",\n") { template ->
                    "    {\"code\": ${jsonString(template.code)}, \"title\": ${jsonString(template.title)}, \"channel\": ${jsonString(template.channel.name)}, \"active\": ${template.active}, \"version\": ${template.version}, \"body\": ${jsonString(template.body)}}"
                },
            )
            appendLine("  ],")
            appendLine("  \"settings\": [")
            appendLine(
                settings.joinToString(",\n") { setting ->
                    "    {\"key\": ${jsonString(setting.key)}, \"value\": ${jsonString(setting.value)}, \"updatedAt\": ${setting.updatedAt}}"
                },
            )
            appendLine("  ]")
            appendLine("}")
        }
    }

    fun buildBackupMetadataJson(): String =
        buildString {
            appendLine("{")
            appendLine("  \"createdAt\": ${jsonString(formatTime(clockMillis()))},")
            appendLine("  \"databaseName\": ${jsonString(RepairAppDatabase.DATABASE_NAME)},")
            appendLine("  \"databaseVersion\": ${RepairAppDatabase.DATABASE_VERSION},")
            appendLine("  \"includes\": [\"database\", \"configuration\", \"app_media\"]")
            appendLine("}")
        }
}
