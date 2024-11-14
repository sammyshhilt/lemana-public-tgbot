package lemana.practice.tgbot.scheduler

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import org.springframework.stereotype.Component
import java.util.Timer
import java.util.TimerTask

@Component
class MessageManager {

    private val timer = Timer("MessageRemovalTimer", true)
    /**
     * @param bot
     * @param chatId
     * @param messageId
     * @param delayMillis
     */
    fun scheduleInlineButtonRemoval(bot: Bot, chatId: Long, messageId: Long, delayMillis: Long) {
        timer.schedule(object : TimerTask() {
            override fun run() {
                bot.editMessageReplyMarkup(ChatId.fromId(chatId), messageId, replyMarkup = null)
            }
        }, delayMillis)
    }

    fun shutdown() {
        timer.cancel()
    }
}
