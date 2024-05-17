package org.bibletranslationtools.fetcher.di

import org.bibletranslationtools.fetcher.config.DevEnvironmentConfig
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.impl.repository.BookCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.BookRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.ChapterCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.DirectoryProviderImpl
import org.bibletranslationtools.fetcher.impl.repository.LangType
import org.bibletranslationtools.fetcher.impl.repository.LanguageRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.PrimaryRepoRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.ProductCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.RequestResourceContainerImpl
import org.bibletranslationtools.fetcher.impl.repository.RCRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.impl.repository.UnfoldingWordLanguagesCatalog
import org.bibletranslationtools.fetcher.io.LocalFileTransferClient
import org.bibletranslationtools.fetcher.repository.BookCatalog
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.PrimaryRepoRepository
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.repository.RequestResourceContainer
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

val appDependencyModule = module(createdAtStart = true) {
    val envConfig: EnvironmentConfig = when (System.getenv("ENVIRONMENT").toUpperCase()) {
        "PRODUCTION" -> EnvironmentConfig()
        "DEVELOPMENT" -> DevEnvironmentConfig()
        else -> throw ExceptionInInitializerError("Environment type is not defined or invalid.")
    }
    single {
        envConfig
    }
    single<DirectoryProvider> { DirectoryProviderImpl(get()) }
    single<StorageAccess> { StorageAccessImpl(get()) }

    single<ChapterCatalog> { ChapterCatalogImpl() }
    single<LanguageCatalog>(named(LangType.GL.name)) { UnfoldingWordLanguagesCatalog(get(), LangType.GL) }
    single<LanguageCatalog>(named(LangType.HL.name)) { UnfoldingWordLanguagesCatalog(get(), LangType.HL) }
    single<LanguageCatalog>(named(LangType.ALL.name)) { UnfoldingWordLanguagesCatalog(get(), LangType.ALL) }
    single<LanguageRepository> {
        LanguageRepositoryImpl(
            get(named(LangType.GL.name)),
            get(named(LangType.HL.name))
        )
    }
    single<ProductCatalog> { ProductCatalogImpl() }
    single<BookCatalog> { BookCatalogImpl() }
    single<BookRepository> { BookRepositoryImpl(get()) }
    single<ResourceContainerRepository> { RCRepositoryImpl(get(), get()) }

    single<IDownloadClient> { LocalFileTransferClient(get()) }
    single<PrimaryRepoRepository>{ PrimaryRepoRepositoryImpl() }
    single<RequestResourceContainer> { RequestResourceContainerImpl(get(), get(), get(), get()) }
}
