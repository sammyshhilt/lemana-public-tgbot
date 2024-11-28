package lemana.practice.tgbot.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "inline_messages")
data class InlineMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val messageId: Long,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY) // Ленивая загрузка, чтобы не тянуть пользователя при каждом запросе
    @JoinColumn(name = "user_id", referencedColumnName = "user_chat_id", nullable = false)
    var user: User? = null // Связь с пользователем (может быть null)
)