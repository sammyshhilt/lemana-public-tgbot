package lemana.practice.tgbot.repo.message

import lemana.practice.tgbot.model.InlineMessage
import lemana.practice.tgbot.model.UserFormsModel
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface InlineMessageRepository : JpaRepository<InlineMessage, Long> {
    fun findByUserChatId(chatId: Long): List<InlineMessage>
    fun findByCreatedAtBefore(expirationTime: LocalDateTime): List<InlineMessage>
    fun deleteByCreatedAtBefore(expirationTime: LocalDateTime)

}