package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Language

interface ChapterCatalog {
    fun getChapterCount(language: Language, book: Book): Int
}
