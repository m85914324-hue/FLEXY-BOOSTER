package com.flexy.app.navigation

enum class Route(val path: String, val title: String) {
    HOME("home", "Home"),
    BOOSTER("booster", "Game Booster"),
    GAMES("games", "Games"),
    VOICE("voice", "Voice Changer"),
    PERFORMANCE("performance", "Performance"),
    SETTINGS("settings", "Settings"),
    ABOUT("about", "About FLEXY"),
    PRIVACY("privacy", "Privacy");

    companion object {
        fun fromPath(path: String?): Route? = entries.firstOrNull { it.path == path }
    }
}
