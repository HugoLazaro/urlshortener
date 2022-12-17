package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

interface ShowShortUrlInfoUseCase {
    fun showShortUrlInfo(id: String) : ShortUrl
}

class ShowShortUrlInfoUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : ShowShortUrlInfoUseCase {
    override fun showShortUrlInfo(id: String) : ShortUrl =
        shortUrlRepository.findByKey(id)?.let {
            val h = shortUrlRepository.findByKey(id)
            print(h)
            return@let h

        } ?: throw ShowShortUrlInfoException(id)
}