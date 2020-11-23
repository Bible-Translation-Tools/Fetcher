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
        contentCache: ContentCacheRepository,
        isGateway: Boolean
    ): List<LanguageViewData> {
        return languages.map {
            val available = if (isGateway) {
                contentCache.isLanguageAvailable(it.code)
            } else it.availability

            LanguageViewData(
                code = it.code,
                anglicizedName = it.anglicizedName,
                localizedName = it.localizedName,
                url = if (available) {
                    "$currentPath/${it.code}"
                } else {
                    null
                }
            )
        }
    }
}
