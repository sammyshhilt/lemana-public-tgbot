package lemana.practice.tgbot.repo.message

import lemana.practice.tgbot.model.TextMessage
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface TextMessageRepository : JpaRepository<TextMessage, Long> {
    fun findByUserChatId(chatId: Long): List<TextMessage>
    fun findByLengthAndUserChatId(length: Int, userChatId: Long): List<TextMessage>
    fun findByCreatedAtBefore(expirationTime: LocalDateTime): List<TextMessage>
    fun deleteByCreatedAtBefore(expirationTime: LocalDateTime)
}