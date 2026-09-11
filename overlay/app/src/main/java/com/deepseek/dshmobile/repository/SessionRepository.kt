package com.deepseek.dshmobile.repository

import com.deepseek.dshmobile.database.MessageEntity
import com.deepseek.dshmobile.database.SessionEntity
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<SessionEntity>>
    suspend fun getSession(sessionId: String): SessionEntity?
    suspend fun createSession(name: String): String
    suspend fun updateSession(sessionId: String, name: String)
    suspend fun deleteSession(sessionId: String)
    suspend fun saveMessage(sessionId: String, role: String, content: String)
    fun getMessages(sessionId: String): Flow<List<MessageEntity>>
}
