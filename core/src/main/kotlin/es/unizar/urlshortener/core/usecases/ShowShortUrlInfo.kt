package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.ShowShortUrlInfoException

/**
 * Get information from the DB of a URL with certain hash
 *
 */
interface ShowShortUrlInfoUseCase {
    fun showShortUrlInfo(id: String): ShortUrl
}

/**
 * Implementation of [ShowShortUrlInfoUseCase]
 */
class ShowShortUrlInfoUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : ShowShortUrlInfoUseCase {
    /**
     * Given a certain hash [id] returns the associated information
     * for that particular URL hash.
     *
     */
    override fun showShortUrlInfo(id: String): ShortUrl =
        shortUrlRepository.findByKey(id)?:throw ShowShortUrlInfoException(id)
}
