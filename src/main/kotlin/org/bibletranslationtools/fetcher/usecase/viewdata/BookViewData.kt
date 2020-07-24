package org.bibletranslationtools.fetcher.usecase.viewdata

data class BookViewData(
    val index: Int,
    val slug: String,
    val anglicizedName: String,
    val localizedName: String,
    val url: String?
) {
    val downloadFileName: String? = if (url != null) java.io.File(url).name else null
}
