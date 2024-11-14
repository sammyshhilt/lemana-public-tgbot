package lemana.practice.tgbot
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
class LemanaBotApplication

fun main(args: Array<String>){
    SpringApplication.run(LemanaBotApplication::class.java, *args)
}