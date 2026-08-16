package com.mangaverse.app.settings.sources.unified

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.json.Json
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.explore.data.SourceAvailabilityRepository
import com.mangaverse.app.extensions.repo.ExternalExtensionRepo
import com.mangaverse.app.extensions.repo.ExternalExtensionRepoRepository
import com.mangaverse.app.extensions.repo.ExternalExtensionType
import com.mangaverse.app.mihon.MihonExtensionManager

class UnifiedSourceCatalogRepositoryTest : FunSpec({

    test("protobuf extension repository keeps its index url") {
        val repository = testRepository()
        val item = repository.invokeToUnifiedRepositoryItem(
            ExternalExtensionRepo(
                type = ExternalExtensionType.MIHON,
                baseUrl = KEIYOUSHI_PROTOBUF_URL,
                name = "Keiyoushi",
                shortName = null,
                website = "https://keiyoushi.github.io",
                signingKeyFingerprint = "fingerprint",
                createdAt = 1L,
                updatedAt = 1L,
                lastSuccessAt = 1L,
                lastError = null,
            ),
        )

        item.url shouldBe KEIYOUSHI_PROTOBUF_URL
    }

    test("preset merge replaces legacy json url and removes duplicate repository ids") {
        val repository = testRepository()
        val legacyItem = unifiedKeiyoushiRepositoryItem(
            name = "Legacy Keiyoushi",
            url = "https://github.com/keiyoushi/extensions/raw/repo/index.min.json",
        )
        val protobufItem = repository.invokeToUnifiedRepositoryItem(
            ExternalExtensionRepo(
                type = ExternalExtensionType.MIHON,
                baseUrl = KEIYOUSHI_PROTOBUF_URL,
                name = "Keiyoushi",
                shortName = null,
                website = "https://keiyoushi.github.io",
                signingKeyFingerprint = "fingerprint",
                createdAt = 2L,
                updatedAt = 2L,
                lastSuccessAt = 2L,
                lastError = null,
            ),
        )

        val matchingItems = repository.invokeWithPresetRepositories(listOf(legacyItem, protobufItem))
            .filter { it.id == KEIYOUSHI_REPOSITORY_ID }

        matchingItems shouldHaveSize 1
        matchingItems.single().url shouldBe KEIYOUSHI_PROTOBUF_URL
        matchingItems.single().isConfigured shouldBe true
        matchingItems.single().isPreset shouldBe true
    }
})

private fun UnifiedSourceCatalogRepository.invokeToUnifiedRepositoryItem(
    repo: ExternalExtensionRepo,
): UnifiedSourceRepositoryItem {
    val method = javaClass.getDeclaredMethod(
        "toUnifiedRepositoryItem",
        ExternalExtensionRepo::class.java,
        Boolean::class.javaPrimitiveType,
    )
    method.isAccessible = true
    return method.invoke(this, repo, false) as UnifiedSourceRepositoryItem
}

@Suppress("UNCHECKED_CAST")
private fun UnifiedSourceCatalogRepository.invokeWithPresetRepositories(
    repositories: List<UnifiedSourceRepositoryItem>,
): List<UnifiedSourceRepositoryItem> {
    val method = javaClass.getDeclaredMethod("withPresetRepositories", List::class.java)
    method.isAccessible = true
    return method.invoke(this, repositories) as List<UnifiedSourceRepositoryItem>
}

private fun testRepository(): UnifiedSourceCatalogRepository {
    return UnifiedSourceCatalogRepository(
        appContext = mockk<Context>(relaxed = true),
        localizedContext = mockk<Context>(relaxed = true),
        database = mockk<MangaDatabase>(relaxed = true),
        settings = mockk<AppSettings>(relaxed = true),
        contentSourcesRepository = mockk<ContentSourcesRepository>(relaxed = true),
        sourceAvailabilityRepository = mockk<SourceAvailabilityRepository>(relaxed = true),
        jsonSourceManager = mockk<com.mangaverse.app.core.jsonsource.JsonSourceManager>(relaxed = true),
        extensionRepoRepository = mockk<ExternalExtensionRepoRepository>(relaxed = true),
        mihonExtensionManager = mockk<MihonExtensionManager>(relaxed = true),
        json = Json,
    )
}

private fun unifiedKeiyoushiRepositoryItem(
    name: String,
    url: String,
): UnifiedSourceRepositoryItem {
    return UnifiedSourceRepositoryItem(
        id = KEIYOUSHI_REPOSITORY_ID,
        kind = UnifiedSourceKind.MIHON,
        name = name,
        url = url,
        locationType = UnifiedRepositoryLocationType.REMOTE_URL,
        website = "https://keiyoushi.github.io",
        isConfigured = true,
        isPreset = false,
        capabilities = emptySet(),
    )
}

private const val KEIYOUSHI_PROTOBUF_URL = "https://github.com/keiyoushi/extensions/raw/repo/index.pb"
private const val KEIYOUSHI_REPOSITORY_ID = "repo:MIHON:https://github.com/keiyoushi/extensions/raw"
