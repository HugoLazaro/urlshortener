package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import org.apache.commons.validator.routines.UrlValidator
import java.nio.charset.StandardCharsets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.safebrowsing.Safebrowsing
import com.google.api.services.safebrowsing.model.*
import es.unizar.urlshortener.core.*
import io.github.g0dkar.qrcode.QRCode
import io.github.g0dkar.qrcode.render.Colors
import java.net.URI
import java.net.URL
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpEntity
import net.minidev.json.JSONObject
import org.springframework.core.io.ByteArrayResource
import org.springframework.web.client.HttpClientErrorException
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.Deque
import java.util.ArrayDeque
import java.net.HttpURLConnection
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import java.io.File



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
    override fun getQR(url: String): Boolean{
       /*
        var safe: Boolean = false
        val restTemplate: RestTemplate = RestTemplate()
        val ResourceUrl: String = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + apiKey;
        val headers: HttpHeaders = HttpHeaders()
        println("La url es " + url)


        */


       /* val requestJson:  JSONObject = json {
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
        }*/

     /*
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
    */

        val dataToEncode = "https://www.youtube.com/watch?v=EE-xtCF3T94&ab_channel=Shaunpants"
        val eachQRCodeSquareSize = 32 // In Pixels!
        val qrCodeRenderer = QRCode(dataToEncode).render(eachQRCodeSquareSize)
        //val qrCodeRenderer = QRCode(dataToEncode).render(eachQRCodeSquareSize,0, Colors.YELLOW, Colors.RED, Colors.PURPLE)

        val qrCodeFile = File("qrcode.png")
        qrCodeFile.outputStream().use { qrCodeRenderer.writeImage(it,"PNG") }

        val imageBytes = ByteArrayOutputStream()
                .also { qrCodeRenderer.writeImage(it) }
                .toByteArray()


        /*val imageOut = ByteArrayOutputStream()

        QRCode(url).render().writeImage(imageOut)

        val imageBytes = imageOut.toByteArray()
        val resource = ByteArrayResource(imageBytes, IMAGE_PNG_VALUE)

        println("This is the QR service: ")
        println(resource)

        FileOutputStream("example01.png").use {
            QRCode("https://github.com/g0dkar/qrcode-kotlin")
                    .render()
                    .writeImage(it)
        }*/

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
