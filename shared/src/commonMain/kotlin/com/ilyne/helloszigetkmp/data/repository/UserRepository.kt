package com.ilyne.helloszigetkmp.data.repository

import com.ilyne.helloszigetkmp.data.api.SzigetApiService
import com.ilyne.helloszigetkmp.data.api.dto.UserDto
import com.ilyne.helloszigetkmp.data.db.entity.UserEntity
import com.ilyne.helloszigetkmp.data.db.dao.UserDao
import com.ilyne.helloszigetkmp.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(
    private val dao: UserDao,
) {
    fun observeUsers(): Flow<List<User>> = dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeUser(id: String): Flow<User?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun getCurrentUser(): User? = dao.getCurrentUser()?.toDomain()

    fun observeCurrentUser(): Flow<User?> = dao.observeCurrentUser().map { it?.toDomain() }

    suspend fun refresh(api: SzigetApiService) {
        val dtos = api.getUsers()
        dao.upsertAll(dtos.map { it.toEntity() })
    }

    suspend fun syncCurrentUser(user: UserDto) {
        dao.upsertAll(listOf(user.toEntity()))
        dao.setCurrentUser(user.id)
    }
}

fun UserDto.toEntity() =
    UserEntity(
        id = id,
        name = name,
        picture = picture,
    )

fun UserEntity.toDomain() =
    User(
        id = id,
        name = name,
        picture = picture,
    )
