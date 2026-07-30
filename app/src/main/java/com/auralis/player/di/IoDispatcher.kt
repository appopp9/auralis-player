package com.auralis.player.di

import javax.inject.Qualifier

/** Qualifier for the IO [kotlinx.coroutines.CoroutineDispatcher]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
