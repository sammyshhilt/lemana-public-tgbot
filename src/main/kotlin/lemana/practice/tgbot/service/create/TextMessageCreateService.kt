package lemana.practice.tgbot.service.create

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import lemana.practice.tgbot.bot.TgBot
import lemana.practice.tgbot.model.TextMessage
import lemana.practice.tgbot.repo.message.TextMessageRepository
import lemana.practice.tgbot.repo.message.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TextMessageCreateService(
    private val textMessageRepository: TextMessageRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun addTextMessage(chatId: Long, messageId: Long, bot: Bot, length: Int) {

        val oldMessages = textMessageRepository.findByLengthAndUserChatId(length, chatId)
        TgBot.logger.info{oldMessages}
        oldMessages.forEach{ message ->
            bot.deleteMessage(ChatId.fromId(message.user!!.chatId), message.messageId)
        }
        textMessageRepository.deleteAll(oldMessages)

        val user = userRepository.findByChatId(chatId)
        TgBot.logger.info { "$user" }
        if (user != null){
            val textMessage = TextMessage(
                messageId = messageId,
                createdAt = LocalDateTime.now(),
                user = user,
                length = length
            )
            textMessageRepository.save(textMessage)
    }
    }
}
