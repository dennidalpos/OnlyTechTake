package com.onlytechtake.repairapp.domain.auth

object AuthPolicy {
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_FAILED_LOGIN_ATTEMPTS = 5
    const val LOGIN_LOCKOUT_MILLIS = 15 * 60 * 1000L
}
