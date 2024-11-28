package lemana.practice.tgbot.service.delete

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import lemana.practice.tgbot.repo.message.InlineMessageRepository
import lemana.practice.tgbot.repo.message.TextMessageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MessageDeleteService(
    private val inlineMessageRepository: InlineMessageRepository,
    private val textMessageRepository: TextMessageRepository,
    private val bot: Bot // Telegram bot instance
) {
    @Transactional
    fun deleteOldInlineMessages(expirationTime: LocalDateTime) {
        val messages = inlineMessageRepository.findByCreatedAtBefore(expirationTime)
        messages.forEach { message ->
            bot.deleteMessage(ChatId.fromId(message.user!!.chatId), message.messageId)
        }
        inlineMessageRepository.deleteByCreatedAtBefore(expirationTime)
    }

    @Transactional
    fun deleteOldTextMessages(expirationTime: LocalDateTime) {
        val messages = textMessageRepository.findByCreatedAtBefore(expirationTime)
        messages.forEach { message ->
            bot.deleteMessage(ChatId.fromId(message.user!!.chatId), message.messageId)
        }
        textMessageRepository.deleteByCreatedAtBefore(expirationTime)
    }
}
