package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()

    override fun isHashUsed(id: String): Boolean = shortUrlEntityRepository.existsById(id) 
    override fun updateSafeInfo(id: String) {
        var newInfoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        println(newInfoUrl)
        newInfoUrl?.properties?.safe = true
        println(newInfoUrl)
        if (newInfoUrl != null)shortUrlEntityRepository.save((newInfoUrl as ShortUrl).toEntity()).toDomain()
    }
    override fun updateReachableInfo(id: String) {
        var newInfoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        println(newInfoUrl)
        newInfoUrl?.properties?.reachable = true
        println(newInfoUrl)
        if (newInfoUrl != null)shortUrlEntityRepository.save((newInfoUrl as ShortUrl).toEntity()).toDomain()
        
    }
    override fun isSafe(id: String): Boolean{
        var infoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain() 
        print("Esto es lo que de" + infoUrl)
        if(infoUrl?.properties?.safe == true){
            return true
        }else{
            return false
        }
    }
    override fun isReachable(id: String): Boolean{
        var infoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain() 
        print("Esto es lo que de" + infoUrl)
        if(infoUrl?.properties?.reachable == true){
            return true
        }else{
            return false
        }
    }
}



