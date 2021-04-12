package org.bibletranslationtools.fetcher.config

/**
 * For Development only
 */
class DevEnvironmentConfig : EnvironmentConfig() {
    override val CDN_BASE_URL: String = "https://audio-content.bibleineverylanguage.org/content"
}
