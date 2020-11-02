package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.data.Deliverable
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.web.controllers.utils.UrlParameters
import java.lang.NumberFormatException

class RCMetadataBuilder(
    parameters: UrlParameters,
    languageCatalog: LanguageCatalog,
    productCatalog: ProductCatalog,
    bookRepository: BookRepository
) {
    val resourceId = parameters.resourceId
    val language = languageCatalog.getLanguage(parameters.languageCode)!!
    val product = productCatalog.getProduct(parameters.productSlug)!!
    val book = bookRepository.getBook(parameters.bookSlug)!!
    val chapter = try {
        parameters.chapter?.toInt()?.let { Chapter(it) }
    } catch (ex: NumberFormatException) {
        null
    }

    fun build(): Deliverable {
        return Deliverable(
            resourceId,
            language,
            book,
            product,
            chapter
        )
    }
}