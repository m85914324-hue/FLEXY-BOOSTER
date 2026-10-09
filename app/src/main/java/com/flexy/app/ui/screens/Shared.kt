package com.flexy.app.ui.screens

import android.content.Context
import com.flexy.app.data.GameItem
import com.flexy.app.flexy

/** Launches a game through Android's normal package-launch intent. */
fun launchGameWithToast(context: Context, game: GameItem, toast: (String) -> Unit) {
    if (!context.flexy.launchGame(game.packageName, game.name)) {
        toast("Couldn't open ${game.name}. Is it still installed?")
    }
}
