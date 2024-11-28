package lemana.practice.tgbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanPostProcessorConfig {

    @Bean
    fun customBeanPostProcessor(): CustomBeanPostProcessor = CustomBeanPostProcessor()
}
