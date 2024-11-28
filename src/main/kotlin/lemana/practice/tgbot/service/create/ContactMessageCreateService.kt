package lemana.practice.tgbot.service.create

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import lemana.practice.tgbot.bot.TgBot
import lemana.practice.tgbot.model.ContactMessage
import lemana.practice.tgbot.repo.message.ContactMessageRepository
import lemana.practice.tgbot.repo.message.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ContactMessageCreateService(
    private val contactMessageRepository: ContactMessageRepository,
    private val userRepository: UserRepository,
) {
    val logger = mu.KotlinLogging.logger("logger")

    @Transactional
    fun addContactMessage(chatId: Long, messageId: Long, phoneNumber: String, bot: Bot) {

        logger.info { "addCOntactMessage started" }

        val oldMessages = contactMessageRepository.findByUserChatId(chatId)
        oldMessages.forEach { message ->
            bot.deleteMessage(ChatId.fromId(message.user!!.chatId), message.messageId)
        }
        contactMessageRepository.deleteAll(oldMessages)

        val user = userRepository.findByChatId(chatId)
        TgBot.logger.info { "$user" }
        if (user != null){
            val contactMessage = ContactMessage(
                messageId = messageId,
                createdAt = LocalDateTime.now(),
                phoneNumber = phoneNumber,
                user = user
            )

            logger.info { "addInlineMessage: $contactMessage" }
            contactMessageRepository.save(contactMessage)
        }

        else{
            logger.info { "no find user with chatId: $chatId" }
        }
    }
}