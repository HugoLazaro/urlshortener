package es.unizar.urlshortener.infrastructure.delivery

import com.google.api.services.safebrowsing.model.*
import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.*
import io.github.g0dkar.qrcode.QRCode
import io.github.g0dkar.qrcode.render.Colors
import net.minidev.json.JSONObject
import org.apache.commons.validator.routines.UrlValidator
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import java.io.ByteArrayOutputStream


class MessageBrokerImpl (
    private val shortUrlRepository: ShortUrlRepositoryService
) :MessageBrokerService{
    @Autowired
    private val template: RabbitTemplate = RabbitTemplate()
    private val safeBrowsingCheck: SafeBrowsingServiceImpl = SafeBrowsingServiceImpl()
    private val isReachableCheck: IsReachableServiceImpl = IsReachableServiceImpl()

    @RabbitListener(queues = ["safeBrowsing"])
    @RabbitHandler
    override fun receiveSafeBrowsingRequest(url: String) {
        var realUrl =  url.split(" ")[0]
        var hash = url.split(" ")[1]
        println(" [x] Received safe broswing'" + realUrl + "'")
        if(safeBrowsingCheck.isSafe(realUrl)){
             shortUrlRepository.updateSafeInfo(hash)
        }
    }
   
    @RabbitListener(queues = ["isReachable"])
    @RabbitHandler
    override fun receiveCheckReachable(url: String) {
        var realUrl =  url.split(" ")[0]
        var hash = url.split(" ")[1]
       println(" [x] Received reachable'" + realUrl + "'")
        if(isReachableCheck.isReachable(realUrl)){
             shortUrlRepository.updateReachableInfo(hash)
        }
    }


    override fun sendSafeBrowsing(type: String, url: String, idHash: String) {
        if(type.equals("safeBrowsing")){
            println(" [x] Sent safe'" + url + "'" );
            this.template.convertAndSend("safeBrowsing", url + " " + idHash)
        }else if(type.equals("isReachable")){
            println(" [x] Sent reachable'" + url + "'" );
            this.template.convertAndSend("isReachable", url + " " + idHash)
        }
        
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
class IsReachableServiceImpl : IsReachableService{
    override fun isReachable(url: String): Boolean{
        //https://stackoverflow.com/questions/29802323/android-with-kotlin-how-to-use-httpurlconnection
        try{
            val myurl : URL = URL(url)
            val huc =  myurl.openConnection()  as HttpURLConnection
            huc.readTimeout = 7000
            huc.connectTimeout = 7000
            
            val responseCode = huc.getResponseCode()
    
            if(HttpURLConnection.HTTP_OK == responseCode){
                return true
            }else if(HttpURLConnection.HTTP_BAD_REQUEST == responseCode){
            }
        }catch(ex: Exception){
              println("La peticion no llega ")
              return false
        }
        return false
    }
}
/**
 * Implementation of the port [SafeBrowsingService].
 */
class SafeBrowsingServiceImpl : SafeBrowsingService {

    private  val apiKey = "AIzaSyAKr96Xa_ri95Tjw7CjRBmdrbAf_hKp7Aw"
    private  val  clientName = "urlshortener"
    private  val clientVersion = "1.5.2"

    fun json(build: JsonObjectBuilder.() -> Unit): JSONObject {
        return JsonObjectBuilder().json(build)
    }

    class JsonObjectBuilder {
        private val deque: Deque<JSONObject> = ArrayDeque()

        fun json(build: JsonObjectBuilder.() -> Unit): JSONObject {
            deque.push(JSONObject())
            this.build()
            return deque.pop()
        }

        infix fun <T> String.To(value: T) {
            deque.peek().put(this, value)
        }
    }

    //https://testsafebrowsing.appspot.com/
    //https://stackoverflow.com/questions/41861449/kotlin-dsl-for-creating-json-objects-without-creating-garbage
    override fun isSafe(url: String): Boolean{
        var safe: Boolean = false
        val restTemplate: RestTemplate = RestTemplate()  
        val ResourceUrl: String = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + apiKey;
        val headers: HttpHeaders = HttpHeaders()
      
        val requestJson:  JSONObject = json {
            "client" To json {
                "clientId" To "urlshortener"
                "clientVersion" To "1.5.2"
            }
            "threatInfo" To json {
                "threatTypes" To arrayOf("MALWARE", "SOCIAL_ENGINEERING", "THREAT_TYPE_UNSPECIFIED",
                                "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION")
                "platformTypes" To "WINDOWS"
                "threatEntryTypes" To "URL"
                "threatEntries" To json {
                    "url" To url
                }
            }
        }
            
        println(requestJson)
        try{
            val entity: HttpEntity<JSONObject> = HttpEntity<JSONObject>(requestJson, headers)
            val response = restTemplate.postForObject(ResourceUrl, entity, JSONObject::class.java)
            //println(response.getHeaders())
            if (response!!.isEmpty()) {
                //println("Pagina segura" + response.getBody())
                safe = true
                println("Pagina segura")
            }else{
                println("Pagina no segura")
            }
            return safe
        } catch (e: HttpClientErrorException ) {
            println("Exception when calling to safebrowsing:")
            println(e)
            return false
        }
    } 
}

/**
 * Implementation of the port [QRService].
 */

class QRServiceImpl : QRService {

    private  val apiKey = "AIzaSyAKr96Xa_ri95Tjw7CjRBmdrbAf_hKp7Aw"
    private  val  clientName = "urlshortener"
    private  val clientVersion = "1.5.2"
    override fun getQR(url: String) : ByteArrayResource =
            ByteArrayOutputStream().let{
                QRCode(url).render().writeImage(it)
                val imageBytes = it.toByteArray()
                ByteArrayResource(imageBytes, IMAGE_PNG_VALUE)
            }
}

/**
 * Implementation of the port [HashService].
 */
@Suppress("UnstableApiUsage")
class HashServiceImpl : HashService {
    override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
}
