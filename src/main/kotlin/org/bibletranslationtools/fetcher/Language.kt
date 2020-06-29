package org.bibletranslationtools.fetcher

data class Language(
    val languageCode: String,
    val anglicizedName: String,
    val localizedName: String
) {
    companion object {
        fun getLanguageByLanguageCode(languageCode: String): Language? {
            // TODO: implement this using langnames.json
            return null
        }
    }
}
