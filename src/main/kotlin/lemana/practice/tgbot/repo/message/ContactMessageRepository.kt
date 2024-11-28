package lemana.practice.tgbot.repo.message
import lemana.practice.tgbot.model.ContactMessage
import org.springframework.data.jpa.repository.JpaRepository


interface ContactMessageRepository : JpaRepository<ContactMessage, Long> {
    fun findByUserChatId(chatId: Long): List<ContactMessage>
    fun deleteAllByUserChatId(chatId: Long): List<ContactMessage>
}
