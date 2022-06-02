package com.munoon.heartbeatlive.server.common

import org.cache2k.Cache2kBuilder

object CacheUtils {
    inline fun <reified K, reified V> cache2kBuilder(): Cache2kBuilder<K, V> =
        Cache2kBuilder.of(K::class.java, V::class.java)
}