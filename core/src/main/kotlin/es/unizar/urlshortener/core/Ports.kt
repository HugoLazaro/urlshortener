package es.unizar.urlshortener.core

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ResponseEntity

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
    fun isHashUsed(id: String): Boolean
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
}

/**
 * [SafeBrowsingService] is the port to the service that validates if an url is secure in order for it to be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface SafeBrowsingService {
    fun isSafe(url: String): Boolean
}

/**
 * [IsReachableService] is the port to the service that validates if an url is reachable in order for it to be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface IsReachableService {
    fun isReachable(url: String): Boolean
}

/**
 * [QRService] is the port to the service that returns a QR from a URI.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface QRService {
    fun getQR(url: String) : ByteArrayResource
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface MessageBrokerService {
    
    fun receiveSafeBrowsingRequest(url: String)

    fun receiveCheckReachable(url: String)

    fun sendSafeBrowsing(type: String, url: String)
}
