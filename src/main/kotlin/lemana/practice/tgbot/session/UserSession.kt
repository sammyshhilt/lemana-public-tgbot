package lemana.practice.tgbot.session


import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import lemana.practice.tgbot.bot.TgBot.Companion.logger
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
@Scope("prototype")
class UserSession(
    val userId: Long,
    val nickname: String,
    var lastMessageId: Long? = null,
    var lastInlineChoice: Long? = null
) {
    var phoneNumber: String = "empty number"
    var temporaryFormList: MutableList<String> = mutableListOf(
        "form 1", "form 2", "form 3", "form 4", "form 5", "form 6", "form 7"
    )

    private val inlineButtonMessageIds = mutableListOf<Long>() // Список ID сообщений
    private val deletionTasks = ConcurrentHashMap<Long, Timer>() // Таймеры на удаление


    fun setLastInlineChoice(chatId: Long, bot: Bot, messageId: Long){
        // lastInlineChoice?.let { bot.deleteMessage(ChatId.fromId(chatId), it) }
        lastInlineChoice = messageId
        logger.info {"setLastInlineChoice: $lastInlineChoice"}
    }


    fun addMessageId(chatId: Long, bot: Bot, messageId: Long) {
        inlineButtonMessageIds.add(messageId)
        //logger.info { "$inlineButtonMessageIds" }
        scheduleRemoval(chatId, bot, messageId, 20 * 1000L) // 10 минут в миллисекундах
    }

    private fun scheduleRemoval(chatId: Long, bot: Bot, messageId: Long, delayMillis: Long) {
        val timer = Timer(true)
        val task = object : TimerTask() {
            override fun run() {
                removeMessageId(chatId, bot, messageId)
            }
        }
        timer.schedule(task, delayMillis)
        deletionTasks[messageId] = timer
    }

    private fun removeMessageId(chatId: Long, bot: Bot, messageId: Long) {

        inlineButtonMessageIds.remove(messageId)
        bot.deleteMessage(ChatId.fromId(chatId), messageId)
        deletionTasks.remove(messageId)?.cancel()
    }

    fun retainLastMessageId(chatId: Long, bot: Bot) {
        if (inlineButtonMessageIds.isNotEmpty()) {
            val lastMessageId = inlineButtonMessageIds.last()

            inlineButtonMessageIds.filter { it != lastMessageId }.forEach { messageId ->
                //bot.editMessageReplyMarkup(ChatId.fromId(chatId), messageId, replyMarkup = null)
                bot.deleteMessage(ChatId.fromId(chatId), messageId)
                deletionTasks.remove(messageId)?.cancel()
            }

            inlineButtonMessageIds.retainAll(listOf(lastMessageId))
        }
    }

    fun shutdown() {
        deletionTasks.values.forEach { it.cancel() }
        deletionTasks.clear()
        inlineButtonMessageIds.clear()
    }

    fun info(): String {
        return "${userId.toString().trimEnd()}\t---\t${nickname}"
    }
}
