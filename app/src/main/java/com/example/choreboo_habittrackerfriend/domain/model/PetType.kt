package com.example.choreboo_habittrackerfriend.domain.model

enum class PetType(val emoji: String, val isPremium: Boolean) {
    FOX("🦊", isPremium = false),
    AXOLOTL("🦎", isPremium = true),
    CAPYBARA("🐹", isPremium = true),
    PANDA("🐼", isPremium = false),
}
