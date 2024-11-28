package lemana.practice.tgbot.service.create

import lemana.practice.tgbot.repo.form.UserFormsRepository
import lemana.practice.tgbot.repo.message.UserRepository
import lemana.practice.tgbot.model.User
import lemana.practice.tgbot.model.UserFormsModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCreateService(
    private val userRepository: UserRepository,
    private val userFormsRepository: UserFormsRepository // Добавляем репозиторий UserFormsRepository
) {

    @Transactional
    fun createUser(nickname: String, chatId: Long, phoneNumber: String): User {
        val existingUser = userRepository.findByChatId(chatId)
        if (existingUser != null) {
            return existingUser
        }

        val newUser = User(
            nickname = nickname,
            chatId = chatId,
            phoneNumber = phoneNumber
        )
        val savedUser = userRepository.save(newUser)

        val defaultForms = (1..10).map { index ->
            UserFormsModel(
                data = "Default Form Data $index",
                user = savedUser
            )
        }
        userFormsRepository.saveAll(defaultForms)

        return savedUser
    }
}
