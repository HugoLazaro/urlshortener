package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import kotlinx.coroutines.runBlocking

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

    override suspend fun isHashUsed(id: String): Boolean = shortUrlEntityRepository.existsById(id) 
    
    override fun updateSafeInfo(id: String, result: Boolean ) {
        var newInfoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        newInfoUrl?.properties?.safe = result
        println("--------Añadida seguridad----------\n" + newInfoUrl)
        if (newInfoUrl != null)shortUrlEntityRepository.save((newInfoUrl as ShortUrl).toEntity()).toDomain()
    }
    override fun updateReachableInfo(id: String, result: Boolean) {
        var newInfoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        newInfoUrl?.properties?.reachable = result
        println("--------Añadida alcanzabilidad----------\n" +newInfoUrl)
        if (newInfoUrl != null)shortUrlEntityRepository.save((newInfoUrl as ShortUrl).toEntity()).toDomain()
        
    }
    override fun isSafe(id: String): Boolean{
        var infoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain() 
        println("\n\n"+infoUrl)
        if(infoUrl?.properties?.safe == true){
            return true
        }else{
            return false
        }
    }
    override fun isReachable(id: String): Boolean{
        var infoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain() 
        if(infoUrl?.properties?.reachable == true){
            return true
        }else{
            return false
        }
    }

     override fun everythingChecked(id: String): Boolean{
        var infoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain() 
        if(infoUrl?.properties?.safe != null && infoUrl?.properties?.reachable != null){
            return true
        }else{
            return false
        }
    }
}



