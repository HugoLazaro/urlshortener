package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties, customUrl: String): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
@Suppress("unused")
class CreateShortUrlUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val validatorService: ValidatorService,
        private val safeBrowsingService: SafeBrowsingService,
        private val isReachableService: IsReachableService,
        private val qrService: QRService,
        private val hashService: HashService,
        private val msgBroker: MessageBrokerService
) : CreateShortUrlUseCase {
    override fun create(url: String, data: ShortUrlProperties, customUrl: String): ShortUrl = runBlocking {
        if (!validatorService.isValid(url)) {
            throw InvalidUrlException(url)
        } else run {

            val id: String = hashService.hasUrl(url, customUrl)
            print(id)

            val isHashUsedCoroutine = async {
                shortUrlRepository.isHashUsed(id)
            }

            val used = isHashUsedCoroutine.await()
            println("usado ha sido :------- $used")

            if (used) {
                throw HashUsedException(id)
            } else {
                val su = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    properties = ShortUrlProperties(
                        ip = data.ip,
                        sponsor = data.sponsor
                    )
                )
                msgBroker.sendSafeBrowsing(url,id)
                shortUrlRepository.save(su)
            }
        }
    }
            

}
