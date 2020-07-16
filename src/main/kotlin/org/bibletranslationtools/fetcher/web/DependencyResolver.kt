package org.bibletranslationtools.fetcher.web

import org.bibletranslationtools.fetcher.impl.repository.*
import org.bibletranslationtools.fetcher.repository.*

object DependencyResolver {
    private val directoryProvider: DirectoryProvider = DirectoryProviderImpl()

    val languageRepository: LanguageRepository = LanguageRepositoryImpl(
        storageAccess =  StorageAccessImpl(directoryProvider),
        languageCatalog = PortGatewayLanguageCatalog()
    )
    val productCatalog: ProductCatalog = ProductCatalogImpl()
    val bookRepository: BookRepository = BookRepositoryImpl(
        storageAccess =  StorageAccessImpl(directoryProvider),
        bookCatalog = BookCatalogImpl()
    )
}