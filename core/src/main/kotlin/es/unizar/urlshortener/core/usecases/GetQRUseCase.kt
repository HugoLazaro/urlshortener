import es.unizar.urlshortener.core.*
import org.springframework.core.io.*

interface GenerateQRUseCase {
    fun generateQR(hash: String) : ByteArrayResource
}

class GetQRUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val qrService: QRService
) : GenerateQRUseCase {
    override fun generateQR(hash: String) : ByteArrayResource =
            shortUrlRepository.findByKey(hash)?.let {
                val shortUrl = shortUrlRepository.findByKey(hash)
                if (shortUrl != null) {
                    qrService.getQR(shortUrl.redirection.target)
                } else {
                    throw QRException(hash)
                }

            } ?: throw QRException(hash)
}