package com.example.digi_diary.di

import java.lang.annotation.Documented
import javax.inject.Qualifier

/**
 * Qualifier for injecting the IO dispatcher.
 */
@Qualifier
@Documented
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher
