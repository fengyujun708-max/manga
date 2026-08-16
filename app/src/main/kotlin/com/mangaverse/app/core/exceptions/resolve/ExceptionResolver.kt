package com.mangaverse.app.core.exceptions.resolve

import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.StringRes
import androidx.collection.MutableScatterMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import com.mangaverse.app.R
import com.mangaverse.app.browser.BrowserActivity
import com.mangaverse.app.core.exceptions.CloudFlareProtectedException
import com.mangaverse.app.core.exceptions.EmptyContentException
import com.mangaverse.app.core.exceptions.InteractiveActionRequiredException
import com.mangaverse.app.core.exceptions.ProxyConfigException
import com.mangaverse.app.core.exceptions.UnsupportedSourceException
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.SourceSettings
import com.mangaverse.app.core.ui.dialog.buildAlertDialog
import com.mangaverse.app.core.util.ext.isHttpUrl
import com.mangaverse.app.core.util.ext.findCloudFlareException
import com.mangaverse.app.core.util.ext.findInteractiveActionRequiredException
import com.mangaverse.app.core.util.ext.restartApplication
import com.mangaverse.app.details.ui.pager.EmptyContentReason
import com.mangaverse.app.parsers.exception.AuthRequiredException
import com.mangaverse.app.parsers.exception.NotFoundException
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.settings.sources.auth.SourceAuthActivity
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.parser.ParserContentRepository
import com.mangaverse.app.core.parser.logUnavailable
import com.mangaverse.app.parsers.ContentParserCredentialsAuthProvider
import com.mangaverse.app.core.model.isLocal
import java.security.cert.CertPathValidatorException
import javax.inject.Inject
import javax.net.ssl.SSLException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ExceptionResolver private constructor(
    private val host: Host,
    private val settings: AppSettings,
    private val mangaRepositoryFactory: ContentRepository.Factory,
    private val captchaAutoResolveCoordinator: CaptchaAutoResolveCoordinator,
) {
    private val continuations = MutableScatterMap<String, Continuation<Boolean>>(1)

    private val browserActionContract = host.registerForActivityResult(BrowserActivity.Contract()) {
        handleActivityResult(BrowserActivity.TAG, it)
    }
    private val sourceAuthContract = host.registerForActivityResult(SourceAuthActivity.Contract()) {
        handleActivityResult(SourceAuthActivity.TAG, it)
    }

    fun showErrorDetails(e: Throwable, url: String? = null) {
        host.router.showErrorDialog(e, url)
    }

    suspend fun resolve(e: Throwable, tryAutoResolve: Boolean = true): Boolean = host.lifecycleScope.async {
        val interactiveAction = e.findInteractiveActionRequiredException()
        val cloudflare = e.findCloudFlareException()
        if (interactiveAction != null) {
            resolveBrowserAction(interactiveAction)
        } else if (cloudflare is CloudFlareProtectedException) {
            resolveCF(cloudflare, tryAutoResolve)
        } else when (e) {
            is AuthRequiredException -> resolveAuthException(e.source)
            is SSLException,
            is CertPathValidatorException -> {
                showSslErrorDialog()
                false
            }

            is ProxyConfigException -> {
                host.router.openProxySettings()
                false
            }

            is NotFoundException -> {
                openInBrowser(e.url)
                false
            }

            is EmptyContentException -> {
                when (e.reason) {
                    EmptyContentReason.NO_CHAPTERS -> openAlternatives(e.manga)
                    EmptyContentReason.LOADING_ERROR -> Unit
                    EmptyContentReason.RESTRICTED -> host.router.openBrowser(e.manga)
                    else -> Unit
                }
                false
            }

            is UnsupportedSourceException -> {
                e.manga?.let { openAlternatives(it) }
                false
            }

            else -> false
        }
    }.await()

    private suspend fun resolveBrowserAction(
        e: InteractiveActionRequiredException
    ): Boolean = suspendCoroutine { cont ->
        continuations[BrowserActivity.TAG] = cont
        browserActionContract.launch(e)
    }

    private suspend fun resolveCF(e: CloudFlareProtectedException, tryAutoResolve: Boolean): Boolean {
        val autoResolveEnabled = tryAutoResolve &&
            (host.context?.let { !SourceSettings(it, e.source).isCaptchaAutoResolveDisabled } ?: true)
        return captchaAutoResolveCoordinator.resolve(
            source = e.source,
            exception = e,
            tryAutomatic = autoResolveEnabled,
        )
    }

    private suspend fun resolveAuthException(source: ContentSource): Boolean {
        if (isCredentialBased(source)) {
            host.router.openSourceSettings(source)
            return false
        }
        return suspendCoroutine { cont ->
            continuations[SourceAuthActivity.TAG] = cont
            sourceAuthContract.launch(source)
        }
    }

    private fun isCredentialBased(source: ContentSource): Boolean {
        if (source.isLocal) return false
        val creation = mangaRepositoryFactory.createWithDiagnostics(source)
        val repo = creation.repository
        if (repo !is ParserContentRepository) {
            creation.logUnavailable("ExceptionResolver", "credential_auth_check_skipped")
            return false
        }
        return repo.getAuthProvider() is ContentParserCredentialsAuthProvider
    }

    fun getResolveStringId(e: Throwable): Int {
        if (e is AuthRequiredException && isCredentialBased(e.source)) {
            return R.string.sign_in_in_settings
        }
        return Companion.getResolveStringId(e)
    }

    private fun openInBrowser(url: String) {
        host.router.openBrowser(url, null, null)
    }

    private fun openAlternatives(manga: Content) {
        host.router.openAlternatives(manga)
    }

    private fun handleActivityResult(tag: String, result: Boolean) {
        continuations.remove(tag)?.resume(result)
    }

    private fun showSslErrorDialog() {
        val ctx = host.context ?: return
        if (settings.isSSLBypassEnabled) {
            Toast.makeText(ctx, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
            return
        }
        buildAlertDialog(ctx) {
            setTitle(R.string.ignore_ssl_errors)
            setMessage(R.string.ignore_ssl_errors_summary)
            setPositiveButton(R.string.apply) { _, _ ->
                settings.isSSLBypassEnabled = true
                Toast.makeText(ctx, R.string.settings_apply_restart_required, Toast.LENGTH_LONG).show()
                ctx.restartApplication()
            }
            setNegativeButton(android.R.string.cancel, null)
        }.show()
    }

    class Factory @Inject constructor(
        private val settings: AppSettings,
        private val mangaRepositoryFactory: ContentRepository.Factory,
        private val captchaAutoResolveCoordinator: CaptchaAutoResolveCoordinator,
    ) {

        fun create(fragment: Fragment) = ExceptionResolver(
            host = Host.FragmentHost(fragment),
            settings = settings,
            mangaRepositoryFactory = mangaRepositoryFactory,
            captchaAutoResolveCoordinator = captchaAutoResolveCoordinator,
        )

        fun create(activity: FragmentActivity) = ExceptionResolver(
            host = Host.ActivityHost(activity),
            settings = settings,
            mangaRepositoryFactory = mangaRepositoryFactory,
            captchaAutoResolveCoordinator = captchaAutoResolveCoordinator,
        )
    }

    private sealed interface Host : ActivityResultCaller, LifecycleOwner {

        val context: Context?

        val router: AppRouter

        val fragmentManager: FragmentManager

        inline fun withContext(block: Context.() -> Unit) {
            context?.apply(block)
        }

        class ActivityHost(val activity: FragmentActivity) : Host,
            ActivityResultCaller by activity,
            LifecycleOwner by activity {

            override val context: Context
                get() = activity

            override val router: AppRouter
                get() = activity.router

            override val fragmentManager: FragmentManager
                get() = activity.supportFragmentManager
        }

        class FragmentHost(val fragment: Fragment) : Host,
            ActivityResultCaller by fragment {

            override val context: Context?
                get() = fragment.context

            override val router: AppRouter
                get() = fragment.router

            override val fragmentManager: FragmentManager
                get() = fragment.childFragmentManager

            override val lifecycle: Lifecycle
                get() = fragment.viewLifecycleOwner.lifecycle
        }
    }

    companion object {

        @StringRes
        fun getResolveStringId(e: Throwable): Int {
            if (e.findCloudFlareException() is CloudFlareProtectedException) {
                return R.string.captcha_solve
            }
            return when (e) {
                is AuthRequiredException -> R.string.sign_in

                is NotFoundException -> if (e.url.isHttpUrl()) R.string.open_in_browser else 0
                is UnsupportedSourceException -> if (e.manga != null) R.string.alternatives else 0
                is SSLException,
                is CertPathValidatorException -> R.string.fix

                is ProxyConfigException -> R.string.settings

                is InteractiveActionRequiredException -> R.string._continue

                is EmptyContentException -> when (e.reason) {
                    EmptyContentReason.RESTRICTED -> if (e.manga.publicUrl.isHttpUrl()) R.string.open_in_browser else 0
                    EmptyContentReason.NO_CHAPTERS -> R.string.alternatives
                    else -> 0
                }

                else -> if (e.findInteractiveActionRequiredException() != null) R.string._continue else 0
            }
        }

        fun canResolve(e: Throwable) = getResolveStringId(e) != 0
    }
}
