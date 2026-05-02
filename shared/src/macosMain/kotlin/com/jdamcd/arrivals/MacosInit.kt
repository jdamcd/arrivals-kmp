package com.jdamcd.arrivals

import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoinMacos() = startKoin {
    modules(commonModule(), module { single<Settings> { NSUserDefaultsSettings() } })
}
