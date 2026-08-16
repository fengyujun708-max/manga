package com.mangaverse.app.core.javascript

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.mangaverse.app.core.network.jsonsource.LegadoHttpClient
import com.mangaverse.app.core.parser.rule.DefaultRuleEngine
import com.mangaverse.app.core.parser.rule.RuleEngine
import javax.inject.Singleton

/**
 * Dagger module for JavaScript engine dependencies
 * 
 * Provides:
 * - JavaScriptEngine (RhinoJavaScriptEngine implementation)
 * - JavaScriptRuleParser
 * - JavaScriptEnginePool
 * - JavaScriptCache
 */
@Module
@InstallIn(SingletonComponent::class)
object JavaScriptModule {
	
	/**
	 * Provides the JavaScript engine implementation
	 * 
	 * Creates RhinoJavaScriptEngine directly to avoid circular dependency
	 */
	@Provides
	@Singleton
	fun provideJavaScriptEngine(
		httpClient: LegadoHttpClient,
		cookieManager: java.net.CookieManager,
		cookieJar: com.mangaverse.app.core.network.jsonsource.PersistentCookieJar,
		@ApplicationContext androidContext: Context
	): JavaScriptEngine {
		android.util.Log.i("JavaScriptModule", "Creating RhinoJavaScriptEngine instance...")
		val engine = RhinoJavaScriptEngine(
			httpClient = httpClient,
			cookieManager = cookieManager,
			cookieJar = cookieJar,
			androidContext = androidContext
		)
		android.util.Log.i("JavaScriptModule", "RhinoJavaScriptEngine instance created successfully")
		return engine
	}
	
	/**
	 * Provides the JavaScript rule parser
	 */
	@Provides
	@Singleton
	fun provideJavaScriptRuleParser(
		engine: JavaScriptEngine
	): JavaScriptRuleParser {
		return JavaScriptRuleParser(engine)
	}
	
	/**
	 * Provides the JSON path parser
	 */
	@Provides
	@Singleton
	fun provideJsonPathParser(): com.mangaverse.app.core.parser.rule.JsonPathParser {
		return com.mangaverse.app.core.parser.rule.JsonPathParser()
	}
	
	/**
	 * Provides the enhanced CSS selector
	 */
	@Provides
	@Singleton
	fun provideEnhancedCssSelector(): com.mangaverse.app.core.parser.rule.EnhancedCssSelector {
		return com.mangaverse.app.core.parser.rule.EnhancedCssSelector()
	}
	
	/**
	 * Provides the enhanced rule engine with JavaScript support
	 */
	@Provides
	@Singleton
	fun provideEnhancedRuleEngine(
		baseRuleEngine: RuleEngine,
		jsRuleParser: JavaScriptRuleParser,
		jsonPathParser: com.mangaverse.app.core.parser.rule.JsonPathParser,
		enhancedCssSelector: com.mangaverse.app.core.parser.rule.EnhancedCssSelector
	): com.mangaverse.app.core.parser.rule.EnhancedRuleEngine {
		android.util.Log.i("JavaScriptModule", "Creating EnhancedRuleEngine with JavaScript support...")
		val engine = com.mangaverse.app.core.parser.rule.EnhancedRuleEngine(
			baseRuleEngine = baseRuleEngine,
			jsRuleParser = jsRuleParser,
			jsonPathParser = jsonPathParser,
			enhancedCssSelector = enhancedCssSelector
		)
		android.util.Log.i("JavaScriptModule", "EnhancedRuleEngine created successfully")
		return engine
	}
}

/**
 * Abstract module for binding interfaces
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class JavaScriptBindingModule {
	
	/**
	 * Binds DefaultRuleEngine to RuleEngine interface
	 */
	@Binds
	abstract fun bindRuleEngine(impl: DefaultRuleEngine): RuleEngine
}
