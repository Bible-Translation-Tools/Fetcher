package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.LanguageViewData

class FetchLanguageViewData(
    private val languageRepo: LanguageRepository,
    private val contentCache: ContentCacheAccessor,
    storage: StorageAccess
) {
    companion object {
        const val DISPLAY_ITEMS_LIMIT = 30
        private const val MATCHING_RESULT_TAKE = 20
    }
    private val languageCodesFromStorage = storage.getLanguageCodes()

    fun getViewDataList(
        currentPath: String
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

    fun filterLanguages(
        query: String,
        currentPath: String,
        currentIndex: Int = 0
    ): List<LanguageViewData> {
        val result = getMatchingLanguages(query, languageRepo.getAll())

        return result
            .drop(currentIndex)
            .take(DISPLAY_ITEMS_LIMIT)
            .map {
                val available = it.code in languageCodesFromStorage ||
                        contentCache.isLanguageAvailable(it.code)
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

    private fun getMatchingLanguages(
        query: String,
        languages: List<Language>
    ): List<Language> {
        val matchingLanguages = mutableSetOf<Language>()

        languages.filter {
            it.code.contains(query.toLowerCase())
        }.forEach {
            matchingLanguages.add(it)
        }

        val choices = languages.flatMap { listOf(it.localizedName, it.anglicizedName) }
        val matchingResult = fuzzyMatching(query, choices, MATCHING_RESULT_TAKE)
        languages.filter {
            it.anglicizedName in matchingResult || it.localizedName in matchingResult
        }.forEach { matchingLanguages.add(it) }

        return matchingLanguages.toList()
    }

    fun loadMoreLanguages(
        currentPath: String,
        currentIndex: Int = 0
    ): List<LanguageViewData> {
        if (currentIndex < 0) return listOf()

        // load more (default) is only applied to HL
        val languages = languageRepo.getHeartLanguages()

        return languages
            .drop(currentIndex)
            .take(DISPLAY_ITEMS_LIMIT)
            .map {
                val available = it.code in languageCodesFromStorage
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
