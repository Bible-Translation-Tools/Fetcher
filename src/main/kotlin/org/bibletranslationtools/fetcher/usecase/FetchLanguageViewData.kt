package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageRepository
import org.bibletranslationtools.fetcher.usecase.viewdata.LanguageViewData

class FetchLanguageViewData(languageRepo: LanguageRepository) {
    private val languages: List<Language> = languageRepo.getLanguages()

    fun getListViewData(currentPath: String): List<LanguageViewData> = languages.map {
        LanguageViewData(
            code = it.code,
            anglicizedName = it.anglicizedName,
            localizedName = it.localizedName,
            url = if (it.availability) "$currentPath/${it.code}" else null
        )
    }
}
