package lemana.practice.tgbot.repo.form

import lemana.practice.tgbot.model.UserFormsModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service

interface UserFormsRepository: JpaRepository<UserFormsModel, Long>{
    fun findAllByUserId(userId: Long): List<UserFormsModel>
}

@Service
class UserFormsService(private val userFormsRepository: UserFormsRepository){}