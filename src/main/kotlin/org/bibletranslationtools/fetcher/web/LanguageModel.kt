package org.bibletranslationtools.fetcher.web

import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageRepository

class LanguageModel(private val languageRepo: LanguageRepository) {
    val viewData: List<Language> = languageRepo.getLanguages()
}