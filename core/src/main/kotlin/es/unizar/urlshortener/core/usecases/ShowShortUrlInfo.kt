package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.ShowShortUrlInfoException

/**
 * Get information from DB of a URL with certain hash
 *
 */
interface ShowShortUrlInfoUseCase {
    fun showShortUrlInfo(id: String): ShortUrl
}

/**
 * Implementation of [ShowShortUrlInfoUseCase]
 *
 * @property shortUrlRepository
 */
class ShowShortUrlInfoUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : ShowShortUrlInfoUseCase {
    /**
     * Given a certain hash [id] returns the associated information
     * for that particular URL hash.
     *
     * @param id
     * @return
     */
    override fun showShortUrlInfo(id: String): ShortUrl =
        shortUrlRepository.findByKey(id)?.let {
            val h = shortUrlRepository.findByKey(id)
            print(h)
            return@let h
        } ?: throw ShowShortUrlInfoException(id)
}
