package org.bibletranslationtools.fetcher.config

/**
 * For Development only
 */
class DevEnvironmentConfig : EnvironmentConfig() {
    override val CDN_BASE_URL: String = "http:/audiobieldev-content.walink.org:8081/content"
}
