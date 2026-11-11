package com.lavaflow.puzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lavaflow.puzzle.ui.LavaShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            val model: ShellViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            val feedback = androidx.compose.runtime.remember { FeedbackController(this) }
            LaunchedEffect(state.profile?.settings) { state.profile?.let { feedback.update(it.settings) } }
            DisposableEffect(feedback) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> feedback.resume()
                        Lifecycle.Event.ON_PAUSE -> { feedback.pause(); model.pauseGame(true) }
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) feedback.resume()
                onDispose { lifecycle.removeObserver(observer); feedback.release() }
            }
            LavaShell(state, model, feedback)
        }
    }
}
