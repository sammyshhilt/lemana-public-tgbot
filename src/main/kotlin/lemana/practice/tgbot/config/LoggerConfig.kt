package lemana.practice.tgbot.config

import mu.KLogger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LoggerConfig {
    @Bean
    fun getLogger(): KLogger = mu.KotlinLogging.logger("ConfigBeanLogger")

}