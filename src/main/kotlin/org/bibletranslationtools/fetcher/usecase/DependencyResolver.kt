package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.impl.repository.BookCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.BookRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.ChapterCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.DirectoryProviderImpl
import org.bibletranslationtools.fetcher.impl.repository.LanguageRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.PortGatewayLanguageCatalog
import org.bibletranslationtools.fetcher.impl.repository.ProductCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess

object DependencyResolver {
    val directoryProvider: DirectoryProvider = DirectoryProviderImpl()

    val storageAccess: StorageAccess = StorageAccessImpl(directoryProvider)
    val languageRepository: LanguageRepository = LanguageRepositoryImpl(
        storageAccess = storageAccess,
        languageCatalog = PortGatewayLanguageCatalog()
    )
    val productCatalog: ProductCatalog = ProductCatalogImpl()
    val bookRepository: BookRepository = BookRepositoryImpl(
        storageAccess = storageAccess,
        bookCatalog = BookCatalogImpl()
    )
    val chapterCatalog: ChapterCatalog = ChapterCatalogImpl()
}