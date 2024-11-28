package lemana.practice.tgbot.scheduler

import lemana.practice.tgbot.service.delete.MessageDeleteService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MessageCleaner(
    private val messageDeleteService: MessageDeleteService
) {
    @Scheduled(fixedRate = 30000) // Проверка каждые 60 секунд
    fun cleanOldMessages() {
        val now = LocalDateTime.now()
        messageDeleteService.deleteOldInlineMessages(now.minusMinutes(2)) // Inline-кнопки старше 10 минут
        // messageDeleteService.deleteOldTextMessages(now.minusMinutes(1)) // Текстовые сообщения старше 10 минут
    }
}
