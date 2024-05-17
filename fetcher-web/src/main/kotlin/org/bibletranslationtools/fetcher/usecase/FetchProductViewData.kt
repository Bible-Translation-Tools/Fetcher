package org.bibletranslationtools.fetcher.usecase

import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.data.Product
import org.bibletranslationtools.fetcher.repository.PrimaryRepoRepository
import org.bibletranslationtools.fetcher.repository.ProductCatalog
import org.bibletranslationtools.fetcher.repository.RequestResourceContainer
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.ProductViewData

class FetchProductViewData(
    productCatalog: ProductCatalog,
    private val storage: StorageAccess,
    private val primaryRepoRepository: PrimaryRepoRepository,
    private val requestResourceContainer: RequestResourceContainer,
    private val languageCode: String
) {
    private val products: List<Product> = productCatalog.getAll()

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

            val hasAudioContent = storage.hasProductContent(languageCode, fileExtensions)

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
        val resourceId = resourceIdByLanguage(languageCode)

        return when {
            requestResourceContainer.getResourceContainer(languageCode, resourceId) != null -> true
            primaryRepoRepository.getRepoUrl(languageCode, resourceId) != null -> true
            else -> false
        }
    }
}
