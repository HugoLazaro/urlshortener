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

    override fun getInfo(id: String): List<Click> {
        val clicks = clickEntityRepository.findAll()
        val clickList = mutableListOf<Click>()
        for (click in clicks) {
            if (click.hash == id) {
                clickList.add(click.toDomain())
            }
        }
        return clickList
    }
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()

    /**
     * Given a hash [id] returns true if it's already used, false otherwise
     *
     * @param id
     * @return
     */
    override suspend fun isHashUsed(id: String): Boolean = shortUrlEntityRepository.existsById(id)

    /**
     * Given a hash [id] returns true if the hash has a sponsor, false otherwise
     *
     * @param id
     * @return
     */
    override fun hasSponsor(id: String): Boolean = shortUrlEntityRepository.findByHash(id)?.sponsor == "true"

    /**
     * Given a hash [id] and a [result] saves the result in the 'safe' field
     * of the given [id] information in the DB
     *
     * @param id
     * @param result
     */
    override fun updateSafeInfo(id: String, result: Boolean) {
        val newInfoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        newInfoUrl?.properties?.safe = result
        println("--------Añadida seguridad----------\n$newInfoUrl")
        if (newInfoUrl != null)shortUrlEntityRepository.save((newInfoUrl).toEntity()).toDomain()
    }

    /**
     * Given a hash [id] and a [result] saves the result in the 'reachable' field
     * of the given [id] information in the DB
     *
     * @param id
     * @param result
     */
    override fun updateReachableInfo(id: String, result: Boolean) {
        val newInfoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        newInfoUrl?.properties?.reachable = result
        println("--------Añadida alcanzabilidad----------\n$newInfoUrl")
        if (newInfoUrl != null)shortUrlEntityRepository.save((newInfoUrl).toEntity()).toDomain()
    }

    /**
     * Given a hash [id] return true if it's safe, false otherwise
     *
     * @param id
     * @return
     */
    override fun isSafe(id: String): Boolean {
        val infoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        println("\n\n" + infoUrl)
        return infoUrl?.properties?.safe == true
    }

    /**
     * Given a hash [id] return true if it's reachable, false otherwise
     *
     * @param id
     * @return
     */
    override fun isReachable(id: String): Boolean {
        val infoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        return infoUrl?.properties?.reachable == true
    }

    /**
     * Given a hash [id] return true if both fields 'safe' and 'reachable'
     * of the hash are different from null
     *
     * @param id
     * @return
     */
    override fun everythingChecked(id: String): Boolean {
        val infoUrl = shortUrlEntityRepository.findByHash(id)?.toDomain()
        return infoUrl?.properties?.safe != null && infoUrl.properties.reachable != null
    }
}
