package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.ContentCacheAccessor
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.LanguageViewData

class FetchLanguageViewData(
    private val languageRepo: LanguageRepository
) {
    fun getGLViewDataList(
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

    fun getHLViewDataList(
        currentPath: String,
        storage: StorageAccess
    ): List<LanguageViewData> {
        val languages = languageRepo.getHeartLanguages()
        val availableLanguageCodes = storage.getLanguageCodes()

        return languages
            .filter {
                it.availability = it.code in availableLanguageCodes
                it.availability
            }
            .map {
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
