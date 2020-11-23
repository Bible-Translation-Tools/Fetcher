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
        isGateway: Boolean = false
    ): List<LanguageViewData> {
        return if (isGateway) {
            gatewayLanguagesViewData(currentPath, contentCache)
        } else {
            heartLanguagesViewData(currentPath)
        }
    }

    private fun gatewayLanguagesViewData(
        path: String,
        contentCache: ContentCacheRepository
    ): List<LanguageViewData> {
        return languages.map {
            val available = contentCache.isLanguageAvailable(it.code)

            LanguageViewData(
                code = it.code,
                anglicizedName = it.anglicizedName,
                localizedName = it.localizedName,
                url = if (available) {
                    "$path/${it.code}"
                } else {
                    null
                }
            )
        }
    }

    private fun heartLanguagesViewData(path: String): List<LanguageViewData> {
        return languages.map {
            LanguageViewData(
                code = it.code,
                anglicizedName = it.anglicizedName,
                localizedName = it.localizedName,
                url = if (it.availability) {
                    "$path/${it.code}"
                } else {
                    null
                }
            )
        }
    }
}
