package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.LanguageViewData

class FetchLanguageViewData(
    private val languageRepo: LanguageRepository
) {
    companion object {
        private const val DISPLAY_ITEMS_LIMIT = 30
        private const val SEARCH_RESULT_TAKE = 20
    }

    fun getViewDataList(
        currentPath: String,
        contentCache: ContentCacheAccessor
    ): List<LanguageViewData> {
        val languages = languageRepo.getGatewayLanguages()

        return languages.map {
            val available = contentCache.isLanguageAvailable(it.code)

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

    fun loadMoreLanguages(
        currentPath: String,
        storage: StorageAccess,
        currentIndex: Int = 0
    ): List<LanguageViewData> {
        val languages = languageRepo.getHeartLanguages()
        val availableLanguageCodes = storage.getLanguageCodes()

        return languages
            .drop(currentIndex)
            .take(DISPLAY_ITEMS_LIMIT)
            .map {
                val available = it.code in availableLanguageCodes
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

    fun filterLanguages(
        query: String,
        currentPath: String,
        storage: StorageAccess
    ): List<LanguageViewData> {
        val heartLanguages = languageRepo.getAll()
        val resultLanguages = mutableSetOf<Language>()

        heartLanguages.filter {
            it.code.contains(query.toLowerCase())
        }.forEach {
            resultLanguages.add(it)
        }

        val choices = heartLanguages.flatMap { listOf(it.localizedName, it.anglicizedName) }
        val matchingResult = fuzzyMatching(query, choices, SEARCH_RESULT_TAKE)

        heartLanguages.filter {
            it.anglicizedName in matchingResult || it.localizedName in matchingResult
        }.forEach { resultLanguages.add(it) }

        val availableLanguageCodes = storage.getLanguageCodes()
        return resultLanguages
            .take(DISPLAY_ITEMS_LIMIT)
            .map {
                LanguageViewData(
                    code = it.code,
                    anglicizedName = it.anglicizedName,
                    localizedName = it.localizedName,
                    url = if (it.code in availableLanguageCodes) {
                        "$currentPath/${it.code}"
                    } else {
                        null
                    }
                )
            }
    }
}
