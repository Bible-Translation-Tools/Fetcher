package org.bibletranslationtools.fetcher.data

import java.io.File

data class Product(
    val slug: String,
    val titleKey: String,
    val descriptionKey: String,
    val icon: File
)
