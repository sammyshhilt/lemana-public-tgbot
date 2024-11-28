package lemana.practice.tgbot.config

import mu.KLogger
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.stereotype.Component

@Component
class CustomBeanFactoryPostProcessor : BeanFactoryPostProcessor {

//    @Autowired
//    lateinit var logger: KLogger
    private val logger = KotlinLogging.logger {}

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        val beanNames = beanFactory.beanDefinitionNames
        logger.debug { "Найдено бинов: ${beanNames.size}" }
    }
}
