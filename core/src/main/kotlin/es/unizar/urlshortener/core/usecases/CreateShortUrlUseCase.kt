package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.HashUsedException
import es.unizar.urlshortener.core.InvalidUrlException
import es.unizar.urlshortener.core.IsReachableService
import es.unizar.urlshortener.core.MessageBrokerService
import es.unizar.urlshortener.core.QRService
import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.SafeBrowsingService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.ValidatorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
        } else {
            run {
                val id: String = hashService.hasUrl(url)
                print(id)

                val su = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    properties = ShortUrlProperties(
                        ip = data.ip,
                        sponsor = data.sponsor
                    )
                )
                msgBroker.sendSafeBrowsing(url, id)
                shortUrlRepository.save(su)

                if (customUrl != "") {
                    launch (Dispatchers.Unconfined){
                        val used = shortUrlRepository.isHashUsed(id, customUrl)
                        if (!used) {
                            msgBroker.sendSafeBrowsing(url, customUrl)
                        }
                    }
                }


                return@runBlocking su
            }
        }
    }
}
