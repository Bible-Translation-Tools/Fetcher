package org.bibletranslationtools.fetcher.data

open class Deliverable(
    val resourceId: String,
    val language: Language,
    val book: Book,
    val product: Product,
    val chapter: Chapter?
)
