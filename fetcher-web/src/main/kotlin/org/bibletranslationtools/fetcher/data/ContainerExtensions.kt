package org.bibletranslationtools.fetcher.data

enum class ContainerExtensions(ext: String) {
    TR("tr");

    companion object : SupportedExtensions {
        override fun isSupported(extension: String): Boolean = entries.any { it.name == extension.uppercase() }
    }
}
