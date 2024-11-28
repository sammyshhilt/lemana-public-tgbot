package lemana.practice.tgbot.session.context
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
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

    fun setLastInlineChoice(chatId: Long, bot: Bot, messageId: Long, session: UserSession){
        session.lastInlineChoice?.let { bot.deleteMessage(ChatId.fromId(chatId), it) }
        session.lastInlineChoice = messageId
    }

    fun clearSession(userId: Long) {
        userSessions.remove(userId)
    }
}
