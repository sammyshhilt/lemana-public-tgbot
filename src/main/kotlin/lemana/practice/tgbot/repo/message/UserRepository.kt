package lemana.practice.tgbot.repo.message

import lemana.practice.tgbot.model.User
import org.springframework.data.jpa.repository.JpaRepository


interface UserRepository : JpaRepository<User, Long> {
    fun findByChatId(chatId: Long): User?
}