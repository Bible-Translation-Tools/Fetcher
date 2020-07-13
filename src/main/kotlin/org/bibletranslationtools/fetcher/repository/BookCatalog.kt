package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Book

interface BookCatalog {
    fun getAll(): List<Book>
}
