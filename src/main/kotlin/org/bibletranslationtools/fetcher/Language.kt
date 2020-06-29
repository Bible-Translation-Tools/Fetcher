package org.bibletranslationtools.fetcher

data class Language(
    val languageCode: String,
    val anglicizedName: String,
    val localizedName: String
) {
    companion object {
        fun getLanguageByLanguageCode(languageCode: String): Language? {
            // TODO: implement this using API to get languages
            return when (languageCode) {
                "en" -> Language("en", "English", "English")
                "fr" -> Language("fr", "French", "français, langue française")
                "vi" -> Language("vi", "Vietnamese", "Ti\u1ebfng Vi\u1ec7t")
                else -> null
            }
        }
    }
}
