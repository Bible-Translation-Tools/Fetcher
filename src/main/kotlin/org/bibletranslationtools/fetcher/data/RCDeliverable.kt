package org.bibletranslationtools.fetcher.data

class RCDeliverable(
    resourceId: String,
    language: Language,
    book: Book,
    product: Product,
    chapter: Chapter?,
    val url: String
) : Deliverable(resourceId, language, book, product, chapter) {
    constructor(deliverable: Deliverable, url: String) : this(
        deliverable.resourceId,
        deliverable.language,
        deliverable.book,
        deliverable.product,
        deliverable.chapter,
        url
    )
}
