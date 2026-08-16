package com.mangaverse.app.main.ui.universe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangaverse.app.R
import com.mangaverse.app.core.api.data.MangaVerseRepository
import com.mangaverse.app.core.api.model.ApiMangaWithRoutes
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.api.toMangaVerseContent
import com.mangaverse.app.main.ui.navigation3.MainNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(
    serverId: String,
    serverName: String,
    onBackClick: () -> Unit,
    mainNavigator: MainNavigator,
    appRouter: AppRouter,
    viewModel: ServerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(serverName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // 打开搜索页，可预设服务器分类
                        appRouter.openSearch(query = "")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜索",
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        androidx.compose.material3.Button(onClick = { }) {
                            Text("重试")
                        }
                    }
                }
            }
            else -> {
                ServerContent(
                    serverId = serverId,
                    serverName = serverName,
                    categories = uiState.categories,
                    hotManga = uiState.hotManga,
                    contentPadding = innerPadding,
                    onOpenManga = { manga ->
                        // 将 MangaVerse 内容转为 Content 并打开详情
                        val content = manga.toMangaVerseContent()
                        content?.let { mainNavigator.openDetails(it) }
                    },
                )
            }
        }
    }
}

@Composable
private fun ServerContent(
    serverId: String,
    serverName: String,
    categories: List<String>,
    hotManga: List<ApiMangaWithRoutes>,
    contentPadding: PaddingValues,
    onOpenManga: (ApiMangaWithRoutes) -> Unit,
) {
    val bottomNav = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + bottomNav + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            ServerHeroHeader(serverName = serverName)
        }
        if (categories.isNotEmpty()) {
            item {
                CategoryChips(categories = categories)
            }
        }
        if (hotManga.isNotEmpty()) {
            item {
                HotMangaSection(
                    title = "热门漫画",
                    mangaList = hotManga,
                    onOpenManga = onOpenManga,
                )
            }
        }
    }
}

@Composable
private fun ServerHeroHeader(serverName: String) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(colors),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = serverName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "探索该服务器的漫画资源",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun CategoryChips(categories: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "分类",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories) { cat ->
                Text(
                    text = cat,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun HotMangaSection(
    title: String,
    mangaList: List<ApiMangaWithRoutes>,
    onOpenManga: (ApiMangaWithRoutes) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(mangaList, key = { it.manga.id ?: it.manga.title }) { manga ->
                MangaCard(
                    manga = manga,
                    onClick = { onOpenManga(manga) },
                )
            }
        }
    }
}

@Composable
private fun MangaCard(
    manga: ApiMangaWithRoutes,
    onClick: () -> Unit,
) {
    val coverUrl = manga.manga.coverUrl.orEmpty()
    androidx.compose.material3.Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 4.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Cover
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                if (coverUrl.isNotEmpty()) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = manga.manga.title.take(1),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                } else {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }
            Text(
                text = manga.manga.title,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (manga.manga.chapterCount > 0) {
                Text(
                    text = "共 ${manga.manga.chapterCount} 话",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val repository: MangaVerseRepository,
) : androidx.lifecycle.ViewModel() {

    data class ServerUiState(
        val isLoading: Boolean = true,
        val categories: List<String> = emptyList(),
        val hotManga: List<ApiMangaWithRoutes> = emptyList(),
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(ServerUiState(isLoading = true))
    val uiState: StateFlow<ServerUiState> = _uiState

    private var currentServerId: String = ""

    fun load(serverId: String) {
        currentServerId = serverId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            // 按服务器分类加载热门
            val category = when (serverId) {
                "copymanga", "baozimh", "manhuagui", "komiic" -> "chinese"
                "batoto", "mangadex", "webtoons" -> "english_korean"
                else -> "all"
            }
            val hotResult = repository.getHotList(category, 20)
            val hotManga = hotResult.getOrNull() ?: emptyList()
            
            // 通用分类标签
            val categories = listOf("热血", "奇幻", "恋爱", "搞笑", "剧情", "冒险", "科幻", "生活")
            
            _uiState.value = ServerUiState(
                isLoading = false,
                categories = categories,
                hotManga = hotManga,
            )
        }
    }
}