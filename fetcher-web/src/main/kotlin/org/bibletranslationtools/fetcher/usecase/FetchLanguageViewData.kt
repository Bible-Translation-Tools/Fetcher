package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.ContentCacheRepository
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.usecase.viewdata.LanguageViewData

class FetchLanguageViewData(
    languageRepo: LanguageRepository
) {
    private val languages: List<Language> = languageRepo.getLanguages()

    fun getListViewData(
        currentPath: String,
        contentCache: ContentCacheRepository
    ): List<LanguageViewData> {
        return languages.map {
            val availableInCache = contentCache.isLanguageAvailable(it.code)
            LanguageViewData(
                code = it.code,
                anglicizedName = it.anglicizedName,
                localizedName = it.localizedName,
                url = if (it.availability || availableInCache) {
                    "$currentPath/${it.code}"
                } else {
                    null
                }
            )
        }
    }
}
