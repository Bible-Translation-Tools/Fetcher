package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ChapterRepository
import java.io.File

class ChapterRepositoryImpl(
    private val chapterCatalog: ChapterCatalog
) : ChapterRepository {

    override fun getAll(languageCode: String, bookSlug: String): List<Chapter> {
        return chapterCatalog.getAll(languageCode, bookSlug)
    }

    override fun requestChapterRC(
        rcFile: File,
        resourceId: String,
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int
    ): File {
        // get the rc from git repo

        // pass into the lib
//        return RCMediaDownloader.download(rcFile, )
        return File("");
    }
}