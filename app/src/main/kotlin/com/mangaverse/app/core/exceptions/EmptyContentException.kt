package com.mangaverse.app.core.exceptions

import com.mangaverse.app.details.ui.pager.EmptyContentReason
import com.mangaverse.app.parsers.model.Content

class EmptyContentException(
    val reason: EmptyContentReason?,
    val manga: Content,
    cause: Throwable?
) : IllegalStateException(cause)
