package lemana.practice.tgbot.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_forms")
data class UserFormsModel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val data: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_chat_id", nullable = true)
    var user: User? = null
)
