package com.onlytechtake.repairapp.features.auth

import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthState
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.auth.LoginResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthGateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun logoutResetsLoadingState() = runTest {
        val fakeRepo = FakeAuthRepository()
        val viewModel = AuthGateViewModel(fakeRepo)

        // Logout resets the form state including loading
        viewModel.logout()
        advanceUntilIdle()

        assertFalse(viewModel.formState.value.loading)
    }

    private class FakeAuthRepository : AuthRepository {
        override val currentUser: StateFlow<AuthenticatedUser?> = MutableStateFlow(null)
        override fun observeAuthState(): Flow<AuthState> = emptyFlow()
        override suspend fun createFirstAdmin(displayName: String, username: String, password: CharArray): AuthenticatedUser {
            error("createFirstAdmin is not expected in logoutResetsLoadingState")
        }
        override suspend fun login(username: String, password: CharArray): LoginResult {
            error("login is not expected in logoutResetsLoadingState")
        }
        override suspend fun loginDemo(): LoginResult {
            error("loginDemo is not expected in logoutResetsLoadingState")
        }

        override suspend fun changePassword(username: String, currentPassword: CharArray, newPassword: CharArray): Result<Unit> {
            error("changePassword is not expected in logoutResetsLoadingState")
        }

        override suspend fun getRecoveryKit(): Result<String> {
            error("getRecoveryKit is not expected in logoutResetsLoadingState")
        }

        override suspend fun resetAdminWithRecoveryCode(code: String, newPassword: CharArray): Result<Unit> {
            error("resetAdminWithRecoveryCode is not expected in logoutResetsLoadingState")
        }

        override fun touchSession() {}
        override suspend fun logout() {}
    }
}
