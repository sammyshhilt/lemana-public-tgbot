package lemana.practice.tgbot.config

import mu.KLogger
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanPostProcessor

class CustomBeanPostProcessor : BeanPostProcessor {

//    @Autowired
//    private lateinit var logger: KLogger
    private val logger = KotlinLogging.logger {}

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? {
        if (beanName.contains("DeleteService")) {
            logger.info { "BeforePost-обработка для бина: $beanName" }
        }
        return bean
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        if (beanName.contains("CreateService")) {
            logger.info { "AfterPost-обработка для бина: $beanName" }
        }
        return bean
    }
}
