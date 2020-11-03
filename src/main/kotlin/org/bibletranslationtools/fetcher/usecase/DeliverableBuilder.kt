package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.data.RCDeliverable
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import java.lang.NumberFormatException

open class DeliverableBuilder(
    private val languageCatalog: LanguageCatalog,
    private val productCatalog: ProductCatalog,
    private val bookRepository: BookRepository
) {
    fun build(parameters: UrlParameters): Deliverable {
        val resourceId = parameters.resourceId
        val language = languageCatalog.getLanguage(parameters.languageCode)!!
        val product = productCatalog.getProduct(parameters.productSlug)!!
        val book = bookRepository.getBook(parameters.bookSlug)!!
        val chapter = try {
            parameters.chapter?.toInt()?.let { Chapter(it) }
        } catch (ex: NumberFormatException) {
            null
        }

        return Deliverable(
            resourceId,
            language,
            book,
            product,
            chapter
        )
    }
}
