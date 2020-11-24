package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.usecase.viewdata.LanguageViewData

class FetchLanguageViewData(
    languageRepo: LanguageRepository
) {
    private val languages: List<Language> = languageRepo.getLanguages()

    fun getGLListViewData(
        currentPath: String,
        contentCache: ContentCacheAccessor
    ): List<LanguageViewData> {
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

    fun getHLListViewData(
        currentPath: String
    ): List<LanguageViewData> {
        return languages.map {
            LanguageViewData(
                code = it.code,
                anglicizedName = it.anglicizedName,
                localizedName = it.localizedName,
                url = if (it.availability) {
                    "$currentPath/${it.code}/${ProductFileExtension.MP3.name.toLowerCase()}"
                } else {
                    null
                }
            )
        }
    }
}
