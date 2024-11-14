package lemana.practice.tgbot.session.context
import lemana.practice.tgbot.session.UserSession
import java.util.concurrent.ConcurrentHashMap

object UserSessionContext {
    private val userSessions: ConcurrentHashMap<Long, UserSession> = ConcurrentHashMap()


    fun getOrCreateSession(userId: Long, nickname: String): UserSession {
        return userSessions.computeIfAbsent(userId) { UserSession(userId, nickname) }
    }

    fun getSession(userId: Long): UserSession? {
        return userSessions[userId]
    }

    fun clearSession(userId: Long) {
        userSessions.remove(userId)
    }
}
