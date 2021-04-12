package org.bibletranslationtools.fetcher.di

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
import org.bibletranslationtools.fetcher.repository.BookCatalog
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

val appDependencyModule = module(createdAtStart = true) {
    single { EnvironmentConfig() }
    single<DirectoryProvider> { DirectoryProviderImpl(get()) }
    single<StorageAccess> { StorageAccessImpl(get()) }

    single<ChapterCatalog> { ChapterCatalogImpl() }
    single<LanguageCatalog>(named("GL")) { PortGatewayLanguageCatalog() }
    single<LanguageCatalog>(named("HL")) { UnfoldingWordHeartLanguagesCatalog() }
    single<LanguageRepository> {
        LanguageRepositoryImpl(
            get(named("GL")),
            get(named("HL"))
        )
    }
    single<ProductCatalog> { ProductCatalogImpl() }
    single<BookCatalog> { BookCatalogImpl() }
    single<BookRepository> { BookRepositoryImpl(get()) }
    single<ResourceContainerRepository> { RCRepositoryImpl(get()) }

    single<IDownloadClient> { LocalFileTransferClient(get()) }

    single {
        ContentAvailabilityCacheBuilder(
            envConfig = get(),
            languageCatalog = get(named("GL")),
            productCatalog = get(),
            chapterCatalog = get(),
            bookRepository = get(),
            rcRepo = get(),
            storageAccess = get()
        )
    }
    single<ContentCacheAccessor> { AvailabilityCacheAccessor(get()) }
}
