package lemana.practice.tgbot.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "contact_messages")
data class ContactMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val messageId: Long,

    @Column(nullable = false)
    val phoneNumber: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_chat_id", nullable = false)
    var user: User? = null
)
