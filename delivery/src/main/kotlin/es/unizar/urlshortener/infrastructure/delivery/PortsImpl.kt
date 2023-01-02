package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.IsReachableService
import es.unizar.urlshortener.core.MessageBrokerService
import es.unizar.urlshortener.core.QRService
import es.unizar.urlshortener.core.SafeBrowsingService
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.ValidatorService
import io.github.cdimascio.dotenv.dotenv
import io.github.g0dkar.qrcode.QRCode
import net.minidev.json.JSONObject
import org.apache.commons.validator.routines.UrlValidator
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

private const val TIME_CONEXION: Int = 7000

/**
 * Implementation of the port [MessageBrokerService]
 */
class MessageBrokerImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val safeBrowsingCheck: SafeBrowsingServiceImpl,
    private val template: RabbitTemplate,
    private val isReachableCheck: IsReachableServiceImpl
) : MessageBrokerService {

    /**
     * Receives the String [url] from the RabbitMQ 'safeBrowsing' Queue, and
     * checks if the url is safe. Then updates the url info
     *
     */
    @RabbitListener(queues = ["safeBrowsing"])
    @RabbitHandler
    override fun receiveSafeBrowsingRequest(url: String) {
        val realUrl = url.split(" ")[0]
        val hash = url.split(" ")[1]
        println(" [x] Received safe broswing'$realUrl'")
        val result = safeBrowsingCheck.isSafe(realUrl)
        shortUrlRepository.updateSafeInfo(hash, result)
    }

    /**
     * Receives the String [url] from the RabbitMQ 'isReachable' Queue and
     * checks if the url is reachable. Then updates the url info
     *
     */
    @RabbitListener(queues = ["isReachable"])
    @RabbitHandler
    override fun receiveCheckReachable(url: String) {
        val realUrl = url.split(" ")[0]
        val hash = url.split(" ")[1]
        println(" [x] Received reachable'$realUrl'")
        val result = isReachableCheck.isReachable(realUrl)
        shortUrlRepository.updateReachableInfo(hash, result)
    }

    /**
     * Given an [url] and its [idHash], sends a message to the Broker
     */
    override fun sendSafeBrowsing(url: String, idHash: String) {
        println(" [x] Sent test reachable and safe'$url'")
        this.template.convertAndSend("tests", "doTests", "$url $idHash")
    }
}

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

    companion object {
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}
class IsReachableServiceImpl : IsReachableService {
    /**
     * Returns the true if the given [url] is reachable, false otherwise
     * Source: https://stackoverflow.com/questions/29802323/android-with-kotlin-how-to-use-httpurlconnection
     */

    override fun isReachable(url: String): Boolean {
        try {
            val myurl = URL(url)
            val huc = myurl.openConnection() as HttpURLConnection
            huc.readTimeout = TIME_CONEXION
            huc.connectTimeout = TIME_CONEXION

            val responseCode = huc.responseCode

            if (HttpURLConnection.HTTP_OK == responseCode) {
                return true
            }
        } catch (ex: Exception) {
            println("La peticion no llega ")
        }
        return false
    }
}

/**
 * Implementation of the port [SafeBrowsingService].
 */
class SafeBrowsingServiceImpl : SafeBrowsingService {
    // https://github.com/cdimascio/dotenv-kotlin
    // var dotenv = dotenv()
    // private val apiKey = dotenv["API_KEY"]
    // private val apiKey = "AIzaSyAKr96Xa_ri95Tjw7CjRBmdrbAf_hKp7Aw"

    private fun json(build: JsonObjectBuilder.() -> Unit): JSONObject {
        return JsonObjectBuilder().json(build)
    }

    class JsonObjectBuilder {
        private val deque: Deque<JSONObject> = ArrayDeque()

        fun json(build: JsonObjectBuilder.() -> Unit): JSONObject {
            deque.push(JSONObject())
            this.build()
            return deque.pop()
        }

        infix fun <T> String.to(value: T) {
            deque.peek()[this] = value
        }
    }

    /**
     * Returns the true if the given [url] is safe, false otherwise
     * Source: https://stackoverflow.com/questions/41861449/kotlin-dsl-for-creating-json-objects-without-creating-garbage
     * Source for testing: https://testsafebrowsing.appspot.com/
     */
    override fun isSafe(url: String): Boolean {
        var safe = false
        val restTemplate = RestTemplate()
        val dotenv = dotenv()
        val apiKey = dotenv["API_KEY"]
        val resourceUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=$apiKey"
        val headers = HttpHeaders()

        val requestJson: JSONObject = json {
            "client" to json {
                "clientId" to "urlshortener"
                "clientVersion" to "1.5.2"
            }
            "threatInfo" to json {
                "threatTypes" to arrayOf(
                    "MALWARE",
                    "SOCIAL_ENGINEERING",
                    "THREAT_TYPE_UNSPECIFIED",
                    "UNWANTED_SOFTWARE",
                    "POTENTIALLY_HARMFUL_APPLICATION"
                )
                "platformTypes" to "WINDOWS"
                "threatEntryTypes" to "URL"
                "threatEntries" to json {
                    "url" to url
                }
            }
        }

        println(requestJson)
        return try {
            val entity: HttpEntity<JSONObject> = HttpEntity<JSONObject>(requestJson, headers)
            val response = restTemplate.postForObject(resourceUrl, entity, JSONObject::class.java)
            if (response!!.isEmpty()) {
                safe = true
                println("Pagina segura")
            } else {
                println("Pagina no segura")
            }
            safe
        } catch (e: HttpClientErrorException) {
            println("Exception when calling to safebrowsing:")
            println(e)
            false
        }
    }
}

/**
 * Implementation of the port [QRService].
 */
class QRServiceImpl : QRService {
    /**
     * Given a [url] returns the QR code Byte Array
     */
    override fun getQR(url: String): ByteArrayResource =
        ByteArrayOutputStream().let {
            QRCode(url).render().writeImage(it)
            val imageBytes = it.toByteArray()
            ByteArrayResource(imageBytes, IMAGE_PNG_VALUE)
        }
}

/**
 * Implementation of the port [HashService].
 */
class HashServiceImpl : HashService {
    override fun hasUrl(url: String, customUrl: String) = if (customUrl == "") {
        Hashing.murmur3_32_fixed().hashString(
            url,
            StandardCharsets.UTF_8
        ).toString()
    } else {
        customUrl
    }
}
