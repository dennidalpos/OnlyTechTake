package com.onlytechtake.repairapp.domain.repositories

import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.*
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun observeTemplates(): Flow<List<Template>>

    fun observeAnchorMappings(): Flow<Map<TemplateAnchor, String>>

    suspend fun setTemplateForAnchor(
        actor: AuthenticatedUser,
        anchor: TemplateAnchor,
        templateCode: String,
    ): Result<Unit>

    fun observeMessageMode(): Flow<MessageMode>

    suspend fun setMessageMode(
        actor: AuthenticatedUser,
        mode: MessageMode,
    ): Result<Unit>

    suspend fun ensureDefaults(actorUserId: String?)

    suspend fun resetToDefaults(actor: AuthenticatedUser): Result<Unit>

    suspend fun saveTemplate(
        actor: AuthenticatedUser,
        code: String,
        title: String,
        body: String,
        active: Boolean,
        step: String? = null,
    ): Result<Unit>

    suspend fun prepareMessage(
        actor: AuthenticatedUser,
        orderId: String,
        anchor: TemplateAnchor,
    ): Result<PreparedMessage>

    suspend fun prepareMessage(
        actor: AuthenticatedUser,
        orderId: String,
        templateCode: String,
        anchor: TemplateAnchor? = null,
    ): Result<PreparedMessage>

    suspend fun recordPrepared(
        actor: AuthenticatedUser,
        prepared: PreparedMessage,
        body: String,
    ): Result<Unit>

    suspend fun confirmSent(
        actor: AuthenticatedUser,
        prepared: PreparedMessage,
        body: String,
    ): Result<Unit>
}
