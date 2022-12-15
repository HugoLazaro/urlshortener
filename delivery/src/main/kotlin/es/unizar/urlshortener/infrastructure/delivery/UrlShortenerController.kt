package es.unizar.urlshortener.infrastructure.delivery

import GenerateQRUseCase
import com.google.common.net.HttpHeaders.CONTENT_TYPE
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.*
import org.apache.http.entity.ContentType.IMAGE_PNG
import org.springframework.core.io.ByteArrayResource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import javax.servlet.http.HttpServletRequest

/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    fun generateQR(hash: String, request: HttpServletRequest) : ResponseEntity<ByteArrayResource>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val customUrl: String,
    val wantQR: Boolean
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)


/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val shortUrlRepository: ShortUrlRepositoryService,
    val generateQRUseCase: GenerateQRUseCase,
    val userAgentInfo: UserAgetInfo
) : UrlShortenerController {
    //https://gist.github.com/c0rp-aubakirov/a4349cbd187b33138969
           
            //val getBrowserAndOS = UserAgentInfoImpl()
            //var y = request.getHeader("User-Agent")
    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            val a = userAgentInfo.getBrowser(request.getHeader("User-Agent"))
            val b = userAgentInfo.getOS(request.getHeader("User-Agent"))
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr,browser = a,platform =b))
            val h = HttpHeaders()
            if (!shortUrlRepository.isSafe(id)) {
                print("Excepcion no segura")
                throw UrlNotSafeException(id)
            } else if (!shortUrlRepository.isReachable(id)) {
                throw UrlNotReachableException(id)
            }else{
                h.location = URI.create(it.target)
                ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))   
            }
        }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties (
                ip = request.remoteAddr,
                sponsor = data.sponsor
            ),
            customUrl = data.customUrl,
            wantQR = data.wantQR
        ).let {
            try {
                // sleep for one second
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (!shortUrlRepository.isSafe(it.hash)) {
                print("Excepcion no segura: -----> " + it.hash)
                throw UrlNotSafeException(data.url)
            } else if (!shortUrlRepository.isReachable(it.hash)) {
                throw UrlNotReachableException(data.url)
            }else{
                val h = HttpHeaders()
                val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
                h.location = url
                val response = ShortUrlDataOut(
                    url = url,
                    properties = mapOf(
                        "safe" to it.properties.safe
                    )
                )
                ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
            }
            
        }

    @GetMapping("/{hash}/qr")
    override fun generateQR(@PathVariable hash: String, request: HttpServletRequest) : ResponseEntity<ByteArrayResource> =
            generateQRUseCase.generateQR(hash).let {
                val h = HttpHeaders()
                h.set(CONTENT_TYPE, IMAGE_PNG.toString())
                ResponseEntity<ByteArrayResource>(it, h, HttpStatus.OK)
            }
}
