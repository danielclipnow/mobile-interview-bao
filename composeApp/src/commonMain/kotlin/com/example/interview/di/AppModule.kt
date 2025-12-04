package com.example.interview.di

import com.example.interview.analytics.Analytics
import com.example.interview.analytics.ConsoleAnalytics
import com.example.interview.api.ApiClient
import com.example.interview.api.ApiClientImpl
import com.example.interview.repository.ProjectRepository
import com.example.interview.screen.projectlist.ProjectListViewModel
import com.example.interview.screen.projectdetail.ProjectDetailViewModel
import com.example.interview.screen.roomdetail.RoomDetailViewModel
import com.example.interview.screen.addoreditcomment.AddOrEditCommentViewModel
import com.example.interview.screen.addoreditroom.AddOrEditRoomViewModel
import com.example.interview.screen.editproject.EditProjectViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin module for the application.
 * Defines all dependency bindings.
 */
val appModule = module {
    // API Client
    single<ApiClient> { ApiClientImpl() }

    // Analytics
    single<Analytics> { ConsoleAnalytics() }

    // Repository - single instance shared across the app
    single { ProjectRepository(apiClient = get(), analytics = get()) }

    // ViewModels
    viewModel { parameters ->
        ProjectListViewModel(
            savedStateHandle = parameters.get(),
            navigator = get(),
            projectRepository = get()
        )
    }

    viewModel { parameters ->
        ProjectDetailViewModel(
            savedStateHandle = parameters.get(),
            navigator = get(),
            projectRepository = get()
        )
    }

    viewModel { parameters ->
        RoomDetailViewModel(
            savedStateHandle = parameters.get(),
            navigator = get(),
            projectRepository = get()
        )
    }

    viewModel { parameters ->
        AddOrEditCommentViewModel(
            savedStateHandle = parameters.get(),
            navigator = get(),
            projectRepository = get()
        )
    }

    viewModel { parameters ->
        AddOrEditRoomViewModel(
            savedStateHandle = parameters.get(),
            navigator = get(),
            projectRepository = get()
        )
    }

    viewModel { parameters ->
        EditProjectViewModel(
            savedStateHandle = parameters.get(),
            navigator = get(),
            projectRepository = get()
        )
    }
}
