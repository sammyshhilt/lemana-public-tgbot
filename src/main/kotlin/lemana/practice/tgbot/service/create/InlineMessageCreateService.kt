package lemana.practice.tgbot.service.create

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import lemana.practice.tgbot.bot.TgBot
import lemana.practice.tgbot.model.InlineMessage
import lemana.practice.tgbot.repo.message.InlineMessageRepository
import lemana.practice.tgbot.repo.message.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class InlineMessageCreateService(
    private val inlineMessageRepository: InlineMessageRepository,
    private val userRepository: UserRepository,
) {
    val logger = mu.KotlinLogging.logger("logger")
    @Transactional
    fun addInlineMessage(chatId: Long, messageId: Long, bot: Bot) {

        //logger.info { "addInlineMessage started" }

        val oldMessages = inlineMessageRepository.findByUserChatId(chatId)
        oldMessages.forEach { message ->
            bot.deleteMessage(ChatId.fromId(message.user!!.chatId), message.messageId)
        }
        inlineMessageRepository.deleteAll(oldMessages)

        val user = userRepository.findByChatId(chatId)
        TgBot.logger.info { "$user" }
        if (user != null){
            val inlineMessage = InlineMessage(
            messageId = messageId,
            createdAt = LocalDateTime.now(),
            user = user
        )

        logger.info { "addInlineMessage: $inlineMessage" }
        inlineMessageRepository.save(inlineMessage)
    }

    else{
        logger.info { "no find user with chatId: $chatId" }
    }
    }
}
