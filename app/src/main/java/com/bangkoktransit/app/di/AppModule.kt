package com.bangkoktransit.app.di

import com.bangkoktransit.app.data.api.BangkokRailwayApi
import com.bangkoktransit.app.data.local.TransitDataCache
import com.bangkoktransit.app.data.local.TripStore
import com.bangkoktransit.app.data.repository.TransitRepository as TransitRepositoryImpl
import com.bangkoktransit.app.domain.repository.SavedTripsRepository
import com.bangkoktransit.app.domain.repository.TransitRepository
import com.bangkoktransit.app.domain.usecase.GetPlaces
import com.bangkoktransit.app.domain.usecase.GetRouteOptions
import com.bangkoktransit.app.domain.usecase.GetStations
import com.bangkoktransit.app.domain.usecase.ManageSavedTrips
import com.bangkoktransit.app.domain.usecase.RefreshStations
import com.bangkoktransit.app.ui.viewmodel.TransitViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { BangkokRailwayApi() }
    single { TransitDataCache(get()) }
    single<TransitRepository> { TransitRepositoryImpl(get(), get()) }
    single<SavedTripsRepository> { TripStore(get()) }

    factory { GetStations(get()) }
    factory { RefreshStations(get()) }
    factory { GetPlaces(get()) }
    factory { GetRouteOptions(get()) }
    factory { ManageSavedTrips(get()) }

    viewModel { TransitViewModel(get(), get(), get(), get(), get()) }
}
