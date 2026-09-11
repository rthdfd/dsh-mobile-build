package com.deepseek.dshmobile.repository

import com.deepseek.dshmobile.database.AppDatabase
import com.deepseek.dshmobile.database.MessageEntity
import com.deepseek.dshmobile.database.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : SessionRepository {

    override fun getAllSessions(): Flow<List<SessionEntity>> =
        database.sessionDao().getAllSessions()

    override suspend fun getSession(sessionId: String): SessionEntity? =
        database.sessionDao().getSession(sessionId)

    override suspend fun createSession(name: String): String {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        database.sessionDao().insertSession(
            SessionEntity(
                sessionId = sessionId,
                name = name,
                createdAt = now,
                updatedAt = now
            )
        )
        return sessionId
    }

    override suspend fun updateSession(sessionId: String, name: String) {
        val session = database.sessionDao().getSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        database.sessionDao().updateSession(session.copy(name = name, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteSession(sessionId: String) {
        database.sessionDao().deleteSessionById(sessionId)
    }

    override suspend fun saveMessage(sessionId: String, role: String, content: String) {
        database.messageDao().insertMessage(
            MessageEntity(
                messageId = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
        val session = database.sessionDao().getSession(sessionId)
        if (session != null) {
            database.sessionDao().updateSession(session.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override fun getMessages(sessionId: String): Flow<List<MessageEntity>> =
        database.messageDao().getMessages(sessionId)
}
