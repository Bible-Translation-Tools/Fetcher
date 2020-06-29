package org.bibletranslationtools.fetcher

data class Book(
    val id: String,
    val anglicizedName: String,
    val localizedName: String,
    val bookNumber: Int
) {
    companion object {
        fun getBookById(id: String): Book? {
            // TODO: implement this using whatever book catalog we end up having
            return null
        }
    }
}
