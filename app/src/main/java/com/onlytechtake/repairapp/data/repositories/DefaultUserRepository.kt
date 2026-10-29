package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.domain.models.User
import com.onlytechtake.repairapp.domain.models.UserRole
import com.onlytechtake.repairapp.domain.repositories.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUserRepository
    @Inject
    constructor(
        private val userDao: UserDao,
    ) : UserRepository {
        override fun observeAll(): Flow<List<User>> = userDao.observeAll().map { list -> list.map { it.toDomain() } }

        override fun observeActiveUsers(): Flow<List<User>> = userDao.observeActiveUsers().map { list -> list.map { it.toDomain() } }

        override suspend fun findById(id: String): User? = userDao.findById(id)?.toDomain()

        override suspend fun findByUsername(username: String): User? = userDao.findByUsername(username)?.toDomain()

        override fun observeById(id: String): Flow<User?> = userDao.observeById(id).map { it?.toDomain() }

        override suspend fun countActiveByRole(role: UserRole): Int = userDao.countActiveByRole(role)
    }
