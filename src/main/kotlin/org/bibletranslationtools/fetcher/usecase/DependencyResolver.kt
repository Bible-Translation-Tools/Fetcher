package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.impl.repository.*
import org.bibletranslationtools.fetcher.repository.*

object DependencyResolver {
    private val directoryProvider: DirectoryProvider = DirectoryProviderImpl()
    private val storageAccess: StorageAccess = StorageAccessImpl(directoryProvider)

    val languageRepository: LanguageRepository = LanguageRepositoryImpl(
        storageAccess = storageAccess,
        languageCatalog = PortGatewayLanguageCatalog()
    )
    val productCatalog: ProductCatalog = ProductCatalogImpl()
    val bookRepository: BookRepository = BookRepositoryImpl(
        storageAccess = storageAccess,
        bookCatalog = BookCatalogImpl()
    )
}
