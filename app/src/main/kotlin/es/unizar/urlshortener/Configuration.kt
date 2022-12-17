package es.unizar.urlshortener

import es.unizar.urlshortener.core.usecases.GetQRUseCaseImpl
import es.unizar.urlshortener.core.usecases.ShowShortUrlInfoUseCaseImpl
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCaseImpl
import es.unizar.urlshortener.core.usecases.LogClickUseCaseImpl
import es.unizar.urlshortener.core.usecases.RedirectUseCaseImpl
import es.unizar.urlshortener.infrastructure.delivery.*
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.core.Binding
import org.springframework.amqp.rabbit.core.RabbitTemplate

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository
) {
    //private  val queueSafeBrowsing = "safeBrowsing"
    //private  val queueIsReachable = "isReachable"

    @Autowired
    private val template: RabbitTemplate =  RabbitTemplate()

    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()
    
    @Bean
    fun safeBrowsingService() = SafeBrowsingServiceImpl()

    @Bean
    fun isReachableService() = IsReachableServiceImpl()
    @Bean
    fun userAgentInfo() = UserAgentInfoImpl() 

    @Bean
    fun getQr() = QRServiceImpl()

    @Bean
    fun generateQRUseCase() = GetQRUseCaseImpl(shortUrlRepositoryService(), getQr())

    @Bean
    fun showShortUrlInfoUseCase() = ShowShortUrlInfoUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun safeBrowsing() = Queue("safeBrowsing") 

    @Bean
    fun reachable() = Queue("isReachable")

    @Bean
    fun exchange():  TopicExchange{
        return TopicExchange("tests")
    }

    @Bean
    fun bindSafeBrowsing(): Binding {
        return BindingBuilder.bind(safeBrowsing()).to(exchange()).with("doTests")
    }

    @Bean
    fun bindReachable(): Binding {
        return BindingBuilder.bind(reachable()).to(exchange()).with("doTests")
    }
    @Bean
    fun messageBroker() = MessageBrokerImpl(shortUrlRepositoryService(),safeBrowsingService(),template,IsReachableServiceImpl())

    
    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), safeBrowsingService(), isReachableService(), getQr(),hashService(), messageBroker())
}