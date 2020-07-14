package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.data.Language

interface ChapterRepository {
    fun getChapters(language: Language, book: Book): List<Chapter>
}
