package lemana.practice.tgbot.session

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
class UserSession(val userId: Long, val nickname: String, var lastMessageId: Long? = null) {

    var phoneNumber: String = "empty number"
    var temporaryFormList: MutableList<String> = mutableListOf("form 1", "form 2", "form 3",
        "form 4", "Form 5", "form 6", "form 7")

    fun info(): String {
        return("${userId.toString().trimEnd()}\t---\t${nickname}")
    }
}