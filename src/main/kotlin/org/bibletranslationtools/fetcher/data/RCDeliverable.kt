package org.bibletranslationtools.fetcher.data

import java.io.File

class RCDeliverable(
    resourceId: String,
    language: Language,
    book: Book,
    product: Product,
    chapter: Chapter?,
    val url: String
): Deliverable(resourceId, language, book, product, chapter)