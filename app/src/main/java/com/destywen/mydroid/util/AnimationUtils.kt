package com.destywen.mydroid.util

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

fun slideInFromRight(durationMs: Int = 250): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth/2 },
        animationSpec = tween(durationMs)
    )

fun slideOutToLeft(durationMs: Int = 250): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(durationMs)
    )

fun slideInFromLeft(durationMs: Int = 250): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(durationMs)
    )

fun slideOutToRight(durationMs: Int = 250): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth/2 },
        animationSpec = tween(durationMs)
    )