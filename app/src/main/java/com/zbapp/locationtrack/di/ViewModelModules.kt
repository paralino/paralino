package com.zbapp.locationtrack.di

import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.tracking.notification.LocationNotificationHandler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val viewModelModules = module {

}

val locationModules = module {
    factory { LocationNotificationHandler(androidContext()) }
    factory { LocationInitHandler(androidContext()) }
}