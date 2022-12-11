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


class MessageBrokerImpl : MessageBrokerService{
    @Autowired
    private val template: RabbitTemplate = RabbitTemplate()
    private val safeBrowsingCheck: SafeBrowsingServiceImpl = SafeBrowsingServiceImpl()
    private val isReachableCheck: IsReachableServiceImpl = IsReachableServiceImpl()

    @RabbitListener(queues = ["safeBrowsing"])
    @RabbitHandler
    override fun receiveSafeBrowsingRequest(url: String) {
        println(" [x] Received '" + url + "'")
        if(safeBrowsingCheck.isSafe(url)){
            println("Es segura");
        }else{
            println("No es segura");
        }
    }

    @RabbitListener(queues = ["isReachable"])
    @RabbitHandler
    override fun receiveCheckReachable(url: String) {
        if(isReachableCheck.isReachable(url)){
            println("Se puede llegar");
        }else{
            println("No llega");
        }
    }


    override fun sendSafeBrowsing(type: String, url: String) {
        if(type.equals("safeBrowsing")){
            println(" [x] Sent '" + url + "'" );
            this.template.convertAndSend("safeBrowsing", url)
        }else if(type.equals("isReachable")){
            println(" [x] Sent '" + url + "'" );
            this.template.convertAndSend("isReachable", url)
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
        println("La url es " + url)
        //https://stackoverflow.com/questions/29802323/android-with-kotlin-how-to-use-httpurlconnection
        try{
            val myurl : URL = URL(url)
            val huc =  myurl.openConnection()  as HttpURLConnection
            huc.readTimeout = 7000
            huc.connectTimeout = 7000
            
            val responseCode = huc.getResponseCode()
    
            if(HttpURLConnection.HTTP_OK == responseCode){
                println("Llega la peticion")
                return true
            }else if(HttpURLConnection.HTTP_BAD_REQUEST == responseCode){
                println("No llega")
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
        println("La url es " + url)
      
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
            println(response)
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

    override fun getQR(url: String): Boolean{

        val dataToEncode = url
        val eachQRCodeSquareSize = 32 // In Pixels!
        val qrCodeRenderer = QRCode(dataToEncode).render(eachQRCodeSquareSize)
        //val qrCodeRenderer = QRCode(dataToEncode).render(eachQRCodeSquareSize,0, Colors.YELLOW, Colors.RED, Colors.PURPLE)

        val qrCodeFile = File("src/main/resources/static/imagenes/qrcode.png")
        //qrCodeFile.outputStream().use { qrCodeRenderer.writeImage(it,"PNG") }
        //TimeUnit.SECONDS.sleep(1);

        return true;
    }
}

/**
 * Implementation of the port [HashService].
 */
@Suppress("UnstableApiUsage")
class HashServiceImpl : HashService {
    override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
}
