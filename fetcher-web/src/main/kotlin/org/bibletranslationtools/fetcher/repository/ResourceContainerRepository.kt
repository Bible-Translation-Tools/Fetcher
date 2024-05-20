package org.bibletranslationtools.fetcher.repository

import java.io.File

interface ResourceContainerRepository {
    fun getRC(
        languageCode: String,
        resourceId: String
    ): File?

    fun downloadRC(
        languageCode: String,
        resourceId: String
    ): File?
}
