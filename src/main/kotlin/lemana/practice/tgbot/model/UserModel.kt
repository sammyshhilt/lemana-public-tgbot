package lemana.practice.tgbot.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val nickname: String,

    @Column(name = "user_chat_id", nullable = false, unique = true)
    val chatId: Long,

    @Column(nullable = true, unique = true)
    val phoneNumber: String,
)
