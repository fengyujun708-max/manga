package com.mangaverse.app.local.domain

import com.mangaverse.app.core.util.MultiMutex
import com.mangaverse.app.parsers.model.Content
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentLock @Inject constructor() : MultiMutex<Content>()
