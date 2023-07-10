package org.bibletranslationtools.fetcher.config

open class EnvironmentConfig {
    open val CONTENT_ROOT_DIR: String = System.getenv("CONTENT_ROOT")
    open val CDN_BASE_URL: String = System.getenv("CDN_BASE_URL")
    open val CDN_BASE_RC_URL: String = System.getenv("CDN_BASE_RC_URL")
    open val CACHE_REFRESH_MINUTES: String = System.getenv("CACHE_REFRESH_MINUTES")
    open val ORATURE_REPO_DIR: String = System.getenv("ORATURE_REPO_DIR")
    open val RC_OUTPUT_DIR: String = System.getenv("RC_TEMP_DIR")
    open val RUNTIME_CONFIG_PROPERTIES: String = System.getenv("RUNTIME_CONFIG_PROPERTIES")
}
