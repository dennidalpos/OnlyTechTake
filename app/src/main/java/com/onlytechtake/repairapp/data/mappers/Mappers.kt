package com.onlytechtake.repairapp.data.mappers

import com.onlytechtake.repairapp.data.local.dao.OrderDetailProjection
import com.onlytechtake.repairapp.data.local.entities.*
import com.onlytechtake.repairapp.domain.models.*

fun CustomerEntity.toDomain(): Customer =
    Customer(
        id = id,
        type = type,
        firstName = firstName,
        lastName = lastName,
        companyName = companyName,
        vatNumber = vatNumber,
        taxCode = taxCode,
        phone = phone,
        email = email,
        address = address,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun RepairOrderEntity.toDomain(): Order =
    Order(
        id = id,
        orderNumber = orderNumber,
        customerId = customerId,
        title = title,
        categoryId = categoryId,
        status = status,
        priority = priority,
        assignedToUserId = assignedToUserId,
        createdByUserId = createdByUserId,
        description = description,
        internalNotes = internalNotes,
        diagnosisOutcome = diagnosisOutcome,
        estimateAmountCents = estimateAmountCents,
        finalAmountCents = finalAmountCents,
        createdAt = createdAt,
        updatedAt = updatedAt,
        closedAt = closedAt,
        closureReason = closureReason,
        customerActionStatus = customerActionStatus,
        closeNote = closeNote,
        closedByUserId = closedByUserId,
        categoryPathSnapshot = categoryPathSnapshot,
        lastCustomerContactAt = lastCustomerContactAt,
    )

fun OrderCategoryEntity.toDomain(): Category =
    Category(
        id = id,
        parentId = parentId,
        level = level,
        name = name,
        description = description,
        active = active,
    )

fun UserEntity.toDomain(): User =
    User(
        id = id,
        displayName = displayName,
        username = username,
        role = role,
        permissions = permissions,
        active = active,
    )

fun TimelineEventEntity.toDomain(): TimelineEvent =
    TimelineEvent(
        id = id,
        orderId = orderId,
        type = type,
        title = title,
        description = description,
        visibleToCustomer = visibleToCustomer,
        createdByUserId = createdByUserId,
        createdAt = createdAt,
    )

fun OrderMediaEntity.toDomain(): OrderMedia =
    OrderMedia(
        id = id,
        orderId = orderId,
        mediaType = mediaType,
        fileName = fileName,
        localPath = localPath,
        mimeType = mimeType,
        capturedAt = capturedAt,
    )

fun SignatureEntity.toDomain(): Signature =
    Signature(
        id = id,
        orderId = orderId,
        signerName = signerName,
        privacyAccepted = privacyAccepted,
        termsAccepted = termsAccepted,
        privacyTemplateVersion = privacyTemplateVersion,
        termsTemplateVersion = termsTemplateVersion,
        fileName = fileName,
        localPath = localPath,
        signedAt = signedAt,
    )

fun OrderDocumentEntity.toDomain(): OrderDocument =
    OrderDocument(
        id = id,
        orderId = orderId,
        type = type,
        localPath = localPath,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        includesCustomerSignature = includesCustomerSignature,
        createdByUserId = createdByUserId,
        createdAt = createdAt,
        sharedAt = sharedAt,
    )

fun MessageTemplateEntity.toDomain(): Template =
    Template(
        id = id,
        code = code,
        title = title,
        channel = channel,
        body = body,
        active = active,
        version = version,
        step = step,
    )

fun MessageLogEntity.toDomain(): MessageLog =
    MessageLog(
        id = id,
        orderId = orderId,
        customerId = customerId,
        channel = channel,
        mode = mode,
        body = body,
        status = status,
        sentAt = sentAt,
        createdAt = createdAt,
    )

fun OrderDetailProjection.toDomain(): OrderDetail =
    OrderDetail(
        order = order.toDomain(),
        customer = customer?.toDomain(),
        category = category?.toDomain(),
        assignedUser = assignedUser?.toDomain(),
        timeline = timeline.map { it.toDomain() },
        media = media.map { it.toDomain() },
        signatures = signatures.map { it.toDomain() },
    )
