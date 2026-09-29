package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.*
import org.bibletranslationtools.fetcher.usecase.viewdata.ProductViewData

class FetchProductViewData(
    productCatalog: ProductCatalog,
    private val storage: StorageAccess,
    private val sourceTextAccessor: SourceTextAccessor,
    private val requestResourceContainer: RequestResourceContainer,
    private val language: Language
) {
    private val products: List<Product> = productCatalog.getAll()
    private val resourceId = resourceIdByLanguage(language.code, language.isGateway)

    fun getListViewData(
        currentPath: String
    ): List<ProductViewData> {
        return products.map {
            val productExtension = ProductFileExtension.getType(it.slug)!!
            val fileExtensions = if (ContainerExtensions.isSupported(productExtension.fileType)) {
                    listOf(ProductFileExtension.BTTR.fileType)
                } else {
                    listOf(ProductFileExtension.MP3.fileType, ProductFileExtension.WAV.fileType)
                }

            val hasAudioContent = storage.hasProductContent(language.code, resourceId, fileExtensions)

            val isAvailable = when (productExtension) {
                ProductFileExtension.ORATURE -> {
                    val hasSourceText = hasSourceText()
                    hasAudioContent && hasSourceText
                }
                else -> hasAudioContent
            }

            ProductViewData(
                slug = it.slug,
                titleKey = it.titleKey,
                descriptionKey = it.descriptionKey,
                iconUrl = it.iconUrl,
                url = if (isAvailable) "$currentPath/${it.slug}" else null
            )
        }
    }

    private fun hasSourceText(): Boolean {
        return when {
            requestResourceContainer.getResourceContainer(language.code, resourceId) != null -> true
            sourceTextAccessor.getRepoUrl(language.code, resourceId) != null -> true
            else -> false
        }
    }
}
