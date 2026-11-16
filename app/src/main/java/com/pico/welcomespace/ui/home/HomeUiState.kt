package com.pico.welcomespace.ui.home

enum class HomeTab(val label: String) {
    SHOP("电商"),
    PLANNER("户型")
}

data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.SHOP
)

sealed interface HomeEvent {
    data class TabSelected(val tab: HomeTab) : HomeEvent
}
