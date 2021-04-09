package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.impl.repository.AvailabilityCacheAccessor
import org.bibletranslationtools.fetcher.impl.repository.ContentAvailabilityCacheBuilder
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.get

object DependencyResolver {
    private val cacheBuilder = ContentAvailabilityCacheBuilder(
        get(EnvironmentConfig::class.java),
        get(LanguageCatalog::class.java, named("GL")),
        get(ChapterCatalog::class.java),
        get(BookRepository::class.java),
        get(StorageAccess::class.java),
        get(ResourceContainerRepository::class.java)
    )
    val contentCache: ContentCacheAccessor = AvailabilityCacheAccessor(
        cacheBuilder
    )
}
