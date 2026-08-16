package com.mangaverse.app.parsers.config

public interface ContentSourceConfig {

	public operator fun <T> get(key: ConfigKey<T>): T
}
