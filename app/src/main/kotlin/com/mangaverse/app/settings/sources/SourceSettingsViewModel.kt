package com.mangaverse.app.settings.sources

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl
import com.mangaverse.app.R
import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.network.cookies.MutableCookieJar
import com.mangaverse.app.core.parser.CachingContentRepository
import com.mangaverse.app.core.parser.JsContentRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.parser.ParserContentRepository
import com.mangaverse.app.core.parser.kotatsu.KotatsuParserRepository
import com.mangaverse.app.core.prefs.SourceSettings
import com.mangaverse.app.core.js.JSSourceParser
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.ui.util.ReversibleAction
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.parsers.ContentParserAuthProvider
import com.mangaverse.app.parsers.ContentParserCredentialsAuthProvider
import com.mangaverse.app.parsers.exception.AuthRequiredException
import com.mangaverse.app.parsers.exception.ParseException
import com.mangaverse.app.core.model.jsonsource.LegadoBookSource
import com.mangaverse.app.core.parser.legado.LegadoRepository
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.mangaverse.app.core.util.ext.EventFlow
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.mangaverse.app.core.network.webview.WebViewExecutor
import kotlinx.coroutines.withContext
import com.mangaverse.app.core.jsonsource.JsonSourceManager
import com.mangaverse.app.core.jsonsource.JsonContentSource

@HiltViewModel
class SourceSettingsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: ContentRepository.Factory,
	private val cookieJar: MutableCookieJar,
	private val mangaSourcesRepository: ContentSourcesRepository,
	private val jsonSourceManager: JsonSourceManager,
	private val jsSourceParser: JSSourceParser,
	private val webViewExecutor: WebViewExecutor,
) : BaseViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

	private val initialSource = ContentSource(savedStateHandle.get<String>(AppRouter.KEY_SOURCE))
	val repository = mangaRepositoryFactory.create(initialSource)
	val source = repository.source

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val username = MutableStateFlow<String?>(null)
	val isAuthorized = MutableStateFlow<Boolean?>(null)
	val browserUrl = MutableStateFlow<String?>(null)
	val isEnabled = mangaSourcesRepository.observeIsEnabled(source)
	private val _jsAccountMeta = MutableStateFlow<JsContentRepository.JsAccountMeta?>(null)
	val jsAccountMeta: StateFlow<JsContentRepository.JsAccountMeta?> = _jsAccountMeta.asStateFlow()
	private val _jsLoginState = MutableEventFlow<Boolean>()
	val jsLoginState: EventFlow<Boolean> = _jsLoginState
	private val _jsWebLoginState = MutableEventFlow<Boolean>()
	val jsWebLoginState: EventFlow<Boolean> = _jsWebLoginState
	private var jsWebLoginJob: Job? = null
	private var usernameLoadJob: Job? = null

	init {
		when (repository) {
			is ParserContentRepository -> {
				browserUrl.value = "https://${repository.domain}"
				repository.getConfig().subscribe(this)
				loadUsername(repository.getAuthProvider())
			}
			is LegadoRepository -> {
				val url = runCatching {
					val json = Json { ignoreUnknownKeys = true; isLenient = true }
					val config = json.decodeFromString<LegadoBookSource>(
						(repository.source as JsonContentSource).entity.config
					)
					config.bookSourceUrl.takeIf { it.isNotBlank() }
				}.getOrNull()
				browserUrl.value = url
			}
			is JsContentRepository -> {
				val url = runCatching {
					val config = (repository.source as? JsonContentSource)?.entity?.config ?: return@runCatching null
					jsSourceParser.parseMetadata(config).getOrNull()?.homepage?.takeIf { it.isNotBlank() }
				}.getOrNull()
				browserUrl.value = url
				loadJsAccountMeta(repository)
			}
			is com.mangaverse.app.mihon.MihonMangaRepository -> {
				val httpSource = (repository.source.catalogueSource as? eu.kanade.tachiyomi.source.online.HttpSource)
				browserUrl.value = httpSource?.baseUrl
			}
			is KotatsuParserRepository -> {
				browserUrl.value = "https://${repository.domain}"
				repository.getConfig().subscribe(this)
			}
		}
	}

	override fun onCleared() {
		when (repository) {
			is ParserContentRepository -> {
				repository.getConfig().unsubscribe(this)
			}
			is KotatsuParserRepository -> {
				repository.getConfig().unsubscribe(this)
			}
		}
		super.onCleared()
	}

	private fun loadJsAccountMeta(repo: JsContentRepository) {
		launchLoadingJob(Dispatchers.Default) {
			runCatching { repo.getJsAccountMeta() }
				.onSuccess { _jsAccountMeta.value = it }
		}
	}

	fun loginJs(username: String, password: String) {
		val repo = repository as? JsContentRepository ?: return
		launchLoadingJob(Dispatchers.IO) {
			val ok = runCatching { repo.jsLogin(username, password) }.getOrDefault(false)
			_jsLoginState.call(ok)
		}
	}

	fun logoutJs() {
		val repo = repository as? JsContentRepository ?: return
		launchLoadingJob(Dispatchers.IO) {
			val ok = runCatching { repo.jsLogout() }.getOrDefault(false)
			_jsLoginState.call(!ok)
			browserUrl.value?.toHttpUrlOrNull()?.let { cookieJar.removeCookies(it) { true } }
		}
	}

	fun setJsCookies(values: Map<String, String>): Boolean {
		val targetUrl = browserUrl.value ?: return false
		val httpUrl = targetUrl.toHttpUrlOrNull() ?: return false
		val cookies = values.mapNotNull { (name, value) ->
			if (name.isBlank() || value.isBlank()) return@mapNotNull null
			runCatching {
				Cookie.Builder()
					.hostOnlyDomain(httpUrl.host)
					.path("/")
					.name(name.trim())
					.value(value)
					.apply { if (httpUrl.isHttps) secure() }
					.build()
			}.getOrNull()
		}
		if (cookies.isEmpty()) return false
		cookieJar.removeCookies(httpUrl) { true }
		cookieJar.saveFromResponse(httpUrl, cookies)
		return true
	}

	suspend fun loginJsWithWebview(): Boolean {
		jsWebLoginJob?.cancel()
		val repo = repository as? JsContentRepository ?: return false
		val meta = jsAccountMeta.value ?: repo.getJsAccountMeta() ?: return false
		val loginUrl = meta.webLoginUrl ?: return false
		jsWebLoginJob = launchLoadingJob(Dispatchers.IO) {
			val ok = runCatching {
				webViewExecutor.loginAndCheck(
					loginUrl = loginUrl,
					checkStatus = { urlNow, titleNow ->
						repo.jsCheckWebLogin(urlNow, titleNow)
					},
					onSuccess = {
						kotlinx.coroutines.runBlocking {
							runCatching { repo.jsNotifyWebLoginSuccess() }.getOrDefault(false)
						}
					},
					cookiesDomain = null,
				)
			}.getOrDefault(false)
			_jsWebLoginState.call(ok)
		}
		return true
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (repository is CachingContentRepository) {
			if (key != SourceSettings.KEY_SLOWDOWN && key != SourceSettings.KEY_SORT_ORDER) {
				repository.invalidateCache()
			}
		}
		if (repository is ParserContentRepository) {
			if (key == SourceSettings.KEY_DOMAIN) {
				browserUrl.value = "https://${repository.domain}"
			}
		}
		if (repository is KotatsuParserRepository) {
			if (key == SourceSettings.KEY_DOMAIN) {
				browserUrl.value = "https://${repository.domain}"
			}
		}
	}

	fun onResume() {
		if (usernameLoadJob?.isActive != true && repository is ParserContentRepository) {
			loadUsername(repository.getAuthProvider())
		}
	}

	fun clearCookies() {
		if (repository !is ParserContentRepository) return
		launchLoadingJob(Dispatchers.Default) {
			val url = HttpUrl.Builder()
				.scheme("https")
				.host(repository.domain)
				.build()
			cookieJar.removeCookies(url, null)
			onActionDone.call(ReversibleAction(R.string.cookies_cleared, null))
			loadUsername(repository.getAuthProvider())
		}
	}

	fun setEnabled(value: Boolean) {
		launchJob(Dispatchers.Default) {
			mangaSourcesRepository.setSourcesEnabled(setOf(source), value)
		}
	}

    private fun loadUsername(authProvider: ContentParserAuthProvider?) {
        launchLoadingJob(Dispatchers.Default) {
            try {
                username.value = null
                isAuthorized.value = null
                isAuthorized.value = authProvider?.isAuthorized()
                username.value = authProvider?.getUsername()
            } catch (_: AuthRequiredException) {
                // 未登录或登录过期，保持为空，不上报错误
            } catch (_: ParseException) {
                // 用户名解析失败（例如站点不返回用户名或需要站点域 Cookie），不影响授权状态展示
            }
        }
    }

    fun loginByCredentials(username: String, password: String) {
        val authProvider = (repository as? ParserContentRepository)?.getAuthProvider() as? ContentParserCredentialsAuthProvider
        if (authProvider == null) return
        launchLoadingJob(Dispatchers.IO) {
            val success = authProvider.login(username, password)
            // 登录成功后刷新授权状态与用户名展示
            val refreshed = (repository as? ParserContentRepository)?.getAuthProvider()
            isAuthorized.value = success
            if (success) {
                // 先用输入的 username 进行乐观更新，避免解析失败导致界面报错
                this@SourceSettingsViewModel.username.value = username
            }
            loadUsername(refreshed)
        }
    }
}
