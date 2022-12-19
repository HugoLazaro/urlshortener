package es.unizar.urlshortener.core

import org.springframework.core.io.ByteArrayResource

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
    suspend fun isHashUsed(id: String): Boolean
    fun updateSafeInfo(id: String, result: Boolean)
    fun updateReachableInfo(id: String, result: Boolean)
    fun isSafe(id: String): Boolean
    fun isReachable(id: String): Boolean
    fun everythingChecked(id: String): Boolean
    fun hasSponsor(id: String): Boolean
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
    fun hasUrl(url: String, customUrl:String): String
}

/**
 * [MessageBrokerService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface MessageBrokerService {
    fun receiveSafeBrowsingRequest(url: String)
    fun receiveCheckReachable(url: String)
    fun sendSafeBrowsing(url: String, idHash:String)
}

