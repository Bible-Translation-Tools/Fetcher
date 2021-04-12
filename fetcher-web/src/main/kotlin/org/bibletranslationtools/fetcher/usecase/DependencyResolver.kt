package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.impl.repository.AvailabilityCacheAccessor
import org.bibletranslationtools.fetcher.impl.repository.BookCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.BookRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.ChapterCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.ContentAvailabilityCacheBuilder
import org.bibletranslationtools.fetcher.impl.repository.DirectoryProviderImpl
import org.bibletranslationtools.fetcher.impl.repository.LanguageRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.PortGatewayLanguageCatalog
import org.bibletranslationtools.fetcher.impl.repository.ProductCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.RCRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.impl.repository.UnfoldingWordHeartLanguagesCatalog
import org.bibletranslationtools.fetcher.io.LocalFileTransferClient
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

object DependencyResolver {
    val environmentConfig = EnvironmentConfig()
    private val directoryProvider: DirectoryProvider = DirectoryProviderImpl(environmentConfig)
    private val gatewayLanguageCatalog: LanguageCatalog = PortGatewayLanguageCatalog()
    private val heartLanguageCatalog: LanguageCatalog = UnfoldingWordHeartLanguagesCatalog()
    val chapterCatalog: ChapterCatalog = ChapterCatalogImpl()

    val storageAccess: StorageAccess = StorageAccessImpl(directoryProvider)
    val languageRepository: LanguageRepository = LanguageRepositoryImpl(
        gatewayLanguageCatalog,
        heartLanguageCatalog
    )
    val productCatalog: ProductCatalog = ProductCatalogImpl()
    val bookRepository: BookRepository = BookRepositoryImpl(
        bookCatalog = BookCatalogImpl()
    )

    val downloadClient: IDownloadClient = LocalFileTransferClient(environmentConfig)
    val rcRepository: ResourceContainerRepository = RCRepositoryImpl(storageAccess)
    private val cacheBuilder = ContentAvailabilityCacheBuilder(
        environmentConfig,
        gatewayLanguageCatalog,
        chapterCatalog,
        bookRepository,
        storageAccess,
        rcRepository
    )
    val contentCache: ContentCacheAccessor = AvailabilityCacheAccessor(
        cacheBuilder
    )
}
