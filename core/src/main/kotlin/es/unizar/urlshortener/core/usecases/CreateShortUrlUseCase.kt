package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
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
    fun create(url: String, data: ShortUrlProperties, customUrl: String, wantQR: Boolean=false): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val validatorService: ValidatorService,
        private val safeBrowsingService: SafeBrowsingService,
        private val isReachableService: IsReachableService,
        private val qrService: QRService,
        private val hashService: HashService,
        private val msgBroker: MessageBrokerService
) : CreateShortUrlUseCase {
    override fun create(url: String, data: ShortUrlProperties, customUrl: String, wantQR: Boolean): ShortUrl = runBlocking {
        if (!validatorService.isValid(url)) {
            throw InvalidUrlException(url)
        } else if(qrService.getQR(url) == null){
            throw UrlNotReachableException(url)
        } else {
    
            val id: String = if (customUrl == "")
                hashService.hasUrl(url)
                else customUrl
                
            val isHashUsedCoroutine = async {
                shortUrlRepository.isHashUsed(id)
            }
    
            val used = isHashUsedCoroutine.await()
            println("usado ha sido :------- " + used)

            if (used) {
                throw HashUsedException(id)
            } else {
                val su = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    properties = ShortUrlProperties(
                            safe = data.safe,
                            ip = data.ip,
                            sponsor = data.sponsor
                    )
                )
                msgBroker.sendSafeBrowsing("safeBrowsing", url,id)
                msgBroker.sendSafeBrowsing("isReachable", url,id)
                shortUrlRepository.save(su)
            }
        }
    }
            

}
