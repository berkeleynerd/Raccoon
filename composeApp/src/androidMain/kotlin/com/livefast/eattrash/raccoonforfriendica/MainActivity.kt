package com.livefast.eattrash.raccoonforfriendica

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.livefast.eattrash.raccoonforfriendica.auth.DefaultAuthManager
import com.livefast.eattrash.raccoonforfriendica.core.navigation.BottomNavigationSection
import com.livefast.eattrash.raccoonforfriendica.core.navigation.di.getDetailOpener
import com.livefast.eattrash.raccoonforfriendica.core.navigation.di.getDrawerCoordinator
import com.livefast.eattrash.raccoonforfriendica.core.navigation.di.getNavigationCoordinator
import com.livefast.eattrash.raccoonforfriendica.domain.identity.repository.di.getAuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var loadingFinished = false
        installSplashScreen().setKeepOnScreenCondition {
            !loadingFinished
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val navigationCoordinator = getNavigationCoordinator()
        val drawerCoordinator = getDrawerCoordinator()

        val backPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // is the drawer is opened, close it
                    if (drawerCoordinator.drawerOpened.value) {
                        lifecycleScope.launch {
                            drawerCoordinator.closeDrawer()
                        }
                        return
                    }

                    // back can be prevented by custom callback
                    val canGoBackCallback = navigationCoordinator.getCanGoBackCallback()
                    check(canGoBackCallback?.invoke() != false) { return }

                    // if in home, ask for confirmation
                    if (navigationCoordinator.currentSection.value == BottomNavigationSection.Home) {
                        // asks for confirmation
                        if (!navigationCoordinator.exitMessageVisible.value) {
                            navigationCoordinator.setExitMessageVisible(true)
                        }
                        return
                    }

                    // goes back to home
                    with(navigationCoordinator) {
                        setCurrentSection(BottomNavigationSection.Home)
                    }
                }
            }
        // when back is detected and the confirmation callback is not active, terminate the activity
        val finishBackPressedCallback =
            object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    navigationCoordinator.setExitMessageVisible(false)
                    finish()
                }
            }
        navigationCoordinator.exitMessageVisible
            .onEach { exitMessageVisible ->
                backPressedCallback.isEnabled = !exitMessageVisible
                finishBackPressedCallback.isEnabled = exitMessageVisible
            }.launchIn(lifecycleScope)
        with(onBackPressedDispatcher) {
            addCallback(backPressedCallback)
            addCallback(finishBackPressedCallback)
        }

        setContent {
            App(
                onLoadingFinished = {
                    loadingFinished = true
                },
            )
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when {
            intent.action == Intent.ACTION_SEND -> handleIncomingAttachment(intent)
            else -> intent.data?.also { handleIncomingUrl(it) }
        }
    }

    private fun handleIncomingUrl(uri: Uri) {
        when {
            // OAuth2 redirect URL
            uri.scheme == DefaultAuthManager.REDIRECT_SCHEME &&
                uri.host == DefaultAuthManager.REDIRECT_HOST -> {
                val authManager = getAuthManager()
                lifecycleScope.launch {
                    runCatching {
                        authManager.performTokenExchange(uri.toString())
                    }
                }
            }

            else -> {
                // try opening deep link URL
                uri.toString().takeUnless { it.isEmpty() }?.also { deeplinkUrl ->
                    lifecycleScope.launch {
                        val navigationCoordinator = getNavigationCoordinator()
                        navigationCoordinator.submitDeeplink(deeplinkUrl)
                    }
                }
            }
        }
    }

    private fun handleIncomingAttachment(intent: Intent) {
        lifecycleScope.launch {
            // if the root navigator has not been set yet, rootNavigator?.push does nothing
            delay(750)
            when {
                "text/plain" == intent.type -> {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.also { content ->
                        val detailOpener = getDetailOpener()
                        detailOpener.openComposer(initialText = content.trim('"'))
                    }
                }

                intent.type?.startsWith("image/") == true -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                    }?.also { uri ->
                        val bytes =
                            contentResolver.openInputStream(uri)?.use {
                                it.readBytes()
                            }
                        if (bytes != null) {
                            val detailOpener = getDetailOpener()
                            detailOpener.openComposer(initialAttachment = bytes)
                        }
                    }
                }
            }
        }
    }
}
