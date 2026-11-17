package com.milauncher.ui

import android.Manifest
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.milauncher.AppNotification
import com.milauncher.LauncherViewModel
import com.milauncher.NotifListener
import com.milauncher.NotesWidgetHost
import com.milauncher.data.AppInfo

@Composable
fun HomeScreen(vm: LauncherViewModel, widgets: NotesWidgetHost) {
    val apps by vm.apps.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val favoriteKeys by vm.favoriteKeys.collectAsState()
    val drawerLetter by vm.drawerLetter.collectAsState()
    val notesOpen by vm.notesOpen.collectAsState()
    val reordering by vm.reordering.collectAsState()
    val calendar by vm.calendar.collectAsState()
    val notificationAccess by vm.notificationAccess.collectAsState()
    val promptDismissed by vm.notificationPromptDismissed.collectAsState()
    val notifications by NotifListener.notifications.collectAsState()
    val nowPlaying by NotifListener.nowPlaying.collectAsState()

    val context = LocalContext.current
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    // Siempre de la A a la Z; "#" arriba solo si hay apps que empiezan por número o símbolo
    val available = remember(apps) { apps.map { it.letter }.toSet() }
    val letters = remember(available) {
        (if ("#" in available) listOf("#") else emptyList()) + ('A'..'Z').map { it.toString() }
    }

    // Permiso de lectura del calendario
    val calendarPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refreshCalendar() }

    // Diálogo de Android "¿Permitir que Mi Launcher cree widgets?"
    val bindWidget = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> widgets.onBindResult(result.resultCode == Activity.RESULT_OK) }

    // Atrás con la lista abierta = volver al inicio
    BackHandler(enabled = drawerLetter != null) { vm.goHome() }
    // Atrás en modo ordenar = terminar
    BackHandler(enabled = reordering) { vm.stopReorder() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Fondo)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            AnimatedContent(
                targetState = drawerLetter == null,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "inicio-o-lista",
            ) { atHome ->
                if (atHome) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            // Deslizar hacia abajo en cualquier parte = bajar notificaciones
                            .swipeDownForNotifications(context, swipeThresholdPx)
                    ) {
                        Spacer(Modifier.fillMaxHeight(0.2f))
                        ClockHeader(nextEvent = calendar.nextEvent, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(16.dp))
                        Column(Modifier.padding(end = 56.dp)) {
                            nowPlaying?.let { np ->
                                NowPlayingRow(np)
                                Spacer(Modifier.height(4.dp))
                            }
                            if (favorites.isEmpty()) {
                                Text(
                                    text = "Toca una letra y mantén pulsada\nuna app para añadirla aquí",
                                    modifier = Modifier.padding(start = 52.dp, top = 8.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Light,
                                    color = TextoSuave,
                                )
                            }
                            FavoritesList(
                                favorites = favorites,
                                reordering = reordering,
                                onToggleFavorite = vm::toggleFavorite,
                                onStartReorder = vm::startReorder,
                                onMove = vm::moveFavorite,
                                onDone = vm::stopReorder,
                                notifications = notifications,
                            )
                            if (!notificationAccess && !promptDismissed && !reordering) {
                                NotificationPrompt(
                                    onEnable = { context.startSafely(NotifListener.settingsIntent(context)) },
                                    onDismiss = vm::dismissNotificationPrompt,
                                )
                            }
                        }
                    }
                } else {
                    AppDrawer(
                        apps = apps,
                        letter = drawerLetter,
                        favoriteKeys = favoriteKeys,
                        notifications = notifications,
                        onToggleFavorite = vm::toggleFavorite,
                        modifier = Modifier.padding(end = 56.dp),
                    )
                }
            }

            AlphabetBar(
                letters = letters,
                available = available,
                selected = drawerLetter,
                onLetter = vm::showLetter,
                onFavorites = vm::goHome,
                onNotebook = vm::openNotes,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 40.dp),
            )
        }

        NotesPanel(
            visible = notesOpen,
            calendar = calendar,
            widgets = widgets,
            onClose = vm::closeNotes,
            onRequestCalendar = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
            onPickWidget = { info -> widgets.choose(info)?.let { bindWidget.launch(it) } },
        )
    }
}

/** Lista completa de apps, desplazada a la letra elegida. */
@Composable
private fun AppDrawer(
    apps: List<AppInfo>,
    letter: String?,
    favoriteKeys: List<String>,
    notifications: Map<String, List<AppNotification>>,
    onToggleFavorite: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Agrupamos por letra: cada grupo empieza con su separador ("B ─────")
    val groups = remember(apps) { apps.groupBy { it.letter }.toList() }
    // Posición en la lista de cada separador (cuenta separadores + apps anteriores)
    val headerIndex = remember(groups) {
        var i = 0
        groups.associate { (l, groupApps) -> (l to i).also { i += 1 + groupApps.size } }
    }

    LaunchedEffect(letter, groups) {
        if (letter == null || groups.isEmpty()) return@LaunchedEffect
        // Si la letra no tiene apps, vamos a la siguiente que sí tenga (o a la última)
        val target = headerIndex[letter]
            ?: groups.firstOrNull { (l, _) -> l != "#" && l > letter }?.let { headerIndex[it.first] }
            ?: headerIndex[groups.last().first]
            ?: 0
        listState.scrollToItem(target)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(top = 120.dp, bottom = 360.dp),
    ) {
        groups.forEach { (groupLetter, groupApps) ->
            item(key = "separador-$groupLetter") {
                LetterSeparator(groupLetter)
            }
            items(groupApps, key = { it.key }) { app ->
                AppRow(
                    app = app,
                    isFavorite = app.key in favoriteKeys,
                    onToggleFavorite = { onToggleFavorite(app) },
                    notifications = notifications[app.packageName].orEmpty(),
                )
            }
        }
    }
}

/** Separador entre letras: la letra pequeña y una línea fina hasta el final. */
@Composable
private fun LetterSeparator(letter: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 16.dp, top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = letter,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 3.sp,
            color = TextoSuave,
        )
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Linea)
        )
    }
}

/** Aviso discreto para activar el acceso a notificaciones (notificaciones + música). */
@Composable
private fun NotificationPrompt(onEnable: () -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.padding(start = 52.dp, top = 24.dp)) {
        Text(
            text = "Ver notificaciones y música aquí",
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            color = TextoSuave,
        )
        Row {
            Text(
                text = "Activar",
                modifier = Modifier
                    .clickable(onClick = onEnable)
                    .padding(end = 20.dp, top = 12.dp, bottom = 12.dp),
                fontSize = 14.sp,
                letterSpacing = 1.sp,
            )
            Text(
                text = "Ahora no",
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = TextoSuave,
            )
        }
    }
}
