package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.LanguageViewData

class FetchLanguageViewData(
    private val languageRepo: LanguageRepository,
    private val storage: StorageAccess
) {
    companion object {
        const val DISPLAY_ITEMS_LIMIT = 30
        private const val MATCHING_RESULT_TAKE = 20
    }

    fun getViewDataList(
        currentPath: String
    ): List<LanguageViewData> {
        val languages = languageRepo.getAll()

        val listViewData = languages.sortedBy { it.isGateway }.map {
            val available = storage.hasLanguageContent(it.code)

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

        val availableLanguages = listViewData.filter { it.url != null }
        val unavailableLanguages = listViewData.filter { it.url == null }

        val allLanguages = availableLanguages + unavailableLanguages

        return allLanguages.take(DISPLAY_ITEMS_LIMIT)
    }

    fun filterLanguages(
        query: String,
        currentPath: String,
        currentIndex: Int = 0
    ): List<LanguageViewData> {
        val result = getMatchingLanguages(query, languageRepo.getAll().sortedBy { it.isGateway })
            .map {
                val available = storage.hasLanguageContent(it.code)

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

        val availableLanguages = result.filter { it.url != null }
        val unavailableLanguages = result.filter { it.url == null }

        val allLanguages = availableLanguages + unavailableLanguages

        return allLanguages
            .drop(currentIndex)
            .take(DISPLAY_ITEMS_LIMIT)
    }

    fun loadMoreLanguages(
        currentPath: String,
        currentIndex: Int = 0
    ): List<LanguageViewData> {
        if (currentIndex < 0) return listOf()

        val listViewData = languageRepo.getAll().sortedBy { it.isGateway }.map {
            val available = storage.hasLanguageContent(it.code)

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

        val availableLanguages = listViewData.filter { it.url != null }
        val unavailableLanguages = listViewData.filter { it.url == null }

        val allLanguages = availableLanguages + unavailableLanguages

        return allLanguages
            .drop(currentIndex + DISPLAY_ITEMS_LIMIT)
            .take(DISPLAY_ITEMS_LIMIT)
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
}
