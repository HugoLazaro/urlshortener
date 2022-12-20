package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.QRException
import es.unizar.urlshortener.core.QRService
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.springframework.core.io.ByteArrayResource

/**
 * Make a QR Code given a certain URL hash
 */
interface GetQRUseCase {
    fun generateQR(hash: String): ByteArrayResource
}

/**
 * Implementation of [GetQRUseCase]
 */
class GetQRUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val qrService: QRService
) : GetQRUseCase {
    /**
     * Given a certain url [hash] return it's correspondent Byte Array
     *
     * @param hash
     * @return
     */
    override fun generateQR(hash: String): ByteArrayResource =
        shortUrlRepository.findByKey(hash)?.let {
            val shortUrl = shortUrlRepository.findByKey(hash)
            if (shortUrl != null) {
                qrService.getQR(shortUrl.redirection.target)
            } else {
                throw QRException(hash)
            }
        } ?: throw QRException(hash)
}
