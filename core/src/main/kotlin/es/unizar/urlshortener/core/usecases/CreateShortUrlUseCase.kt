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
        } else if (!safeBrowsingService.isSafe(url)) {
            throw UrlNotSafeException(url)
        } else if (!isReachableService.isReachable(url)) {
            throw UrlNotReachableException(url)
        } else{
            if(wantQR){
                if(!qrService.getQR(url)){
                    throw UrlNotReachableException(url)
                }
            }
            msgBroker.sendSafeBrowsing("safeBrowsing", url)
            val id: String = if (customUrl == "")
                hashService.hasUrl(url)
            else customUrl
            /*launch (Dispatchers.Unconfined) {
                val used : Boolean = shortUrlRepository.isHashUsed(id)
                print(used)
                if (used) {
                    throw HashUsedException(id)
                }
            }*/

            val isHashUsedCoroutine = async {
                shortUrlRepository.isHashUsed(id)
            }

            val used = isHashUsedCoroutine.await()
            print("usado ha sido :------- " + used)
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
                shortUrlRepository.save(su)
            }

            }
        }

}