package org.bibletranslationtools.fetcher.usecase

import me.xdrop.fuzzywuzzy.FuzzySearch

fun fuzzyMatching(keyword: String, choices: List<String>, take: Int = 10): List<String> {
    return FuzzySearch.extractTop(keyword, choices, take).map {
        it.string
    }
}

fun resourceIdByLanguage(languageId: String, isGateway: Boolean): String {
    return when {
        languageId == "id" -> "ayt"
        languageId == "arb" -> "avd"
        isGateway -> "ulb"
        else -> "reg"
    }
}