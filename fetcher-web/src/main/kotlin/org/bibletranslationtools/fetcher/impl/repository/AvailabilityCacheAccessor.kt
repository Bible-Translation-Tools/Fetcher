package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.usecase.cache.AvailabilityCache

class AvailabilityCacheAccessor(
    private val cacheBuilder: ContentAvailabilityCacheBuilder
) : ContentCacheAccessor {

    private var cache: AvailabilityCache

    init {
        cache = cacheBuilder.build()
    }

    @Synchronized
    override fun update() {
        cache = cacheBuilder.build()
    }

    @Synchronized
    override fun isLanguageAvailable(code: String) = cache.languages.any { it.code == code && it.availability }

    @Synchronized
    override fun isProductAvailable(productSlug: String, languageCode: String): Boolean {
        return cache.languages.find {
            it.code == languageCode && it.availability
        }?.products?.any {
            it.slug == productSlug && it.availability
        } ?: false
    }

    @Synchronized
    override fun isBookAvailable(
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): Boolean {
        val productCache = cache.languages.find {
            it.code == languageCode && it.availability
        }?.products?.find {
            it.slug == productSlug && it.availability
        } ?: return false

        return productCache.books.any {
            it.slug == bookSlug && it.availability
        }
    }

    @Synchronized
    override fun getChapterUrl(
        number: Int,
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): String? {
        val bookCache = cache.languages.find {
            it.code == languageCode && it.availability
        }?.products?.find {
            it.slug == productSlug && it.availability
        }?.books?.find {
            it.slug == bookSlug && it.availability
        } ?: return null

        return bookCache.chapters.find {
            it.number == number
        }?.url
    }

    @Synchronized
    override fun getBookUrl(
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): String? {
        return cache.languages.find {
            it.code == languageCode && it.availability
        }?.products?.find {
            it.slug == productSlug && it.availability
        }?.books?.find {
            it.slug == bookSlug
        }?.url
    }
}
