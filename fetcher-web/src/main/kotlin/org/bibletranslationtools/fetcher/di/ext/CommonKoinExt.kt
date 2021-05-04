package org.bibletranslationtools.fetcher.di.ext

import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

/**
 * Provides dependency resolution globally.
 *
 * Koin Application must be started/registered
 * before using this object.
 */
object CommonKoinExt {
    /**
     * Retrieve given dependency
     * @param qualifier - bean canonicalName / optional
     * @param parameters - dependency parameters / optional
     */
    inline fun <reified T : Any> get(
        qualifier: Qualifier? = null,
        noinline parameters: ParametersDefinition? = null
    ) = getKoin().get<T>(qualifier, parameters)

    /**
     * Retrieve given dependency lazily
     * @param qualifier - bean canonicalName / optional
     * @param parameters - dependency parameters / optional
     */
    inline fun <reified T : Any> inject(
        qualifier: Qualifier? = null,
        noinline parameters: ParametersDefinition? = null
    ) =
        lazy { get<T>(qualifier, parameters) }

    /**
     * Retrieve Koin instance
     */
    fun getKoin() = GlobalContext.get()
}
