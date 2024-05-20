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

    private val comparator = compareBy(LanguageViewData::isGateway)
        .then(compareByDescending { it.url != null })

    fun getViewDataList(
        currentPath: String
    ): List<LanguageViewData> {
        val languages = languageRepo.getAll()

        return languages
            .map {
                val available = storage.hasLanguageContent(it.code)

                LanguageViewData(
                    code = it.code,
                    anglicizedName = it.anglicizedName,
                    localizedName = it.localizedName,
                    isGateway = it.isGateway,
                    url = if (available) {
                        "$currentPath/${it.code}"
                    } else {
                        null
                    }
                )
            }
            .sortedWith(comparator)
            .take(DISPLAY_ITEMS_LIMIT)
    }

    fun filterLanguages(
        query: String,
        currentPath: String,
        currentIndex: Int = 0
    ): List<LanguageViewData> {
        return getMatchingLanguages(query, languageRepo.getAll())
            .map {
                val available = storage.hasLanguageContent(it.code)

                LanguageViewData(
                    code = it.code,
                    anglicizedName = it.anglicizedName,
                    localizedName = it.localizedName,
                    isGateway = it.isGateway,
                    url = if (available) {
                        "$currentPath/${it.code}"
                    } else {
                        null
                    }
                )
            }
            .sortedWith(comparator)
            .drop(currentIndex)
            .take(DISPLAY_ITEMS_LIMIT)
    }

    fun loadMoreLanguages(
        currentPath: String,
        currentIndex: Int = 0
    ): List<LanguageViewData> {
        if (currentIndex < 0) return listOf()

        return languageRepo.getAll()
            .map {
                val available = storage.hasLanguageContent(it.code)

                LanguageViewData(
                    code = it.code,
                    anglicizedName = it.anglicizedName,
                    localizedName = it.localizedName,
                    isGateway = it.isGateway,
                    url = if (available) {
                        "$currentPath/${it.code}"
                    } else {
                        null
                    }
                )
            }
            .sortedWith(comparator)
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
