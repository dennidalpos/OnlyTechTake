package com.onlytechtake.repairapp.domain.repositories

import android.graphics.Bitmap
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.Signature

interface SignatureRepository {
    suspend fun saveSignature(
        actor: AuthenticatedUser,
        orderId: String,
        signerName: String,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        bitmap: Bitmap,
    ): Result<Signature>

    suspend fun deleteSignatureFile(signature: Signature)
}
