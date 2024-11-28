package lemana.practice.tgbot.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "text_messages")
data class TextMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val messageId: Long,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val length: Int,

    @ManyToOne(fetch = FetchType.LAZY) // Ленивая загрузка
    @JoinColumn(name = "user_id", referencedColumnName = "user_chat_id", nullable = true)
    var user: User? = null // Связь с пользователем (может быть null)
)
