package com.auralis.player.di

import javax.inject.Qualifier

/**
 * Qualifier for an application-scoped [kotlinx.coroutines.CoroutineScope] tied to the
 * process lifetime. Used by repositories that need to keep StateFlows alive independent
 * of any UI subscription.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
