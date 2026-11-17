@file:OptIn(ExperimentalMaterial3Api::class)

package com.futbolmenajer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futbolmenajer.engine.Economy
import com.futbolmenajer.engine.Scheduler
import com.futbolmenajer.ui.screens.*
import kotlinx.coroutines.launch

private data class NavItem(val screen: Screen, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Screen.HOME, Icons.Filled.Home), NavItem(Screen.INBOX, Icons.Filled.Mail), NavItem(Screen.SQUAD, Icons.Filled.Groups),
    NavItem(Screen.TACTICS, Icons.Filled.Tune), NavItem(Screen.TRAINING, Icons.Filled.FitnessCenter), NavItem(Screen.TRANSFERS, Icons.Filled.SwapHoriz),
    NavItem(Screen.FIXTURES, Icons.Filled.DateRange), NavItem(Screen.TABLES, Icons.Filled.Leaderboard), NavItem(Screen.COMPETITIONS, Icons.Filled.EmojiEvents),
    NavItem(Screen.YOUTH, Icons.Filled.School), NavItem(Screen.FACILITIES, Icons.Filled.Domain), NavItem(Screen.FINANCE, Icons.Filled.AttachMoney),
    NavItem(Screen.CLUB, Icons.Filled.Flag), NavItem(Screen.NATIONAL, Icons.Filled.Public), NavItem(Screen.SAVE_LOAD, Icons.Filled.Save)
)

private val gameScreens = setOf(Screen.HOME, Screen.INBOX, Screen.SQUAD, Screen.TACTICS, Screen.TRAINING, Screen.TRANSFERS, Screen.FIXTURES,
    Screen.TABLES, Screen.COMPETITIONS, Screen.YOUTH, Screen.FACILITIES, Screen.FINANCE, Screen.CLUB, Screen.NATIONAL, Screen.PLAYER,
    Screen.CLUB_VIEW, Screen.MATCH_REPORT, Screen.SAVE_LOAD, Screen.PRE_MATCH)

@Composable
fun App(vm: GameViewModel) {
    val v = vm.version   // yeniden çizim tetikleyicisi
    val snackbar = remember { SnackbarHostState() }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm.toast) {
        vm.toast?.let { snackbar.showSnackbar(it); vm.toast = null }
    }
    BackHandler(enabled = drawer.isOpen || vm.screen != Screen.MENU) {
        if (drawer.isOpen) scope.launch { drawer.close() } else vm.back()
    }

    if (vm.busy) { BusyScreen(vm); return }

    val gs = vm.gs
    val inGame = gs != null && gs.userClubId >= 0 && vm.screen in gameScreens

    ModalNavigationDrawer(
        drawerState = drawer, gesturesEnabled = inGame,
        drawerContent = {
            if (inGame && gs != null) ModalDrawerSheet(drawerContainerColor = Color(0xFF102219)) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Column(Modifier.fillMaxWidth().background(PitchDark).padding(16.dp)) {
                        Text(gs.userClub.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Text(gs.managerName, color = Muted)
                        Text(Scheduler.dateStr(gs.season, gs.day), color = Gold, fontSize = 12.sp)
                    }
                    navItems.forEach { item ->
                        val unread = if (item.screen == Screen.INBOX) gs.inbox.count { !it.read } else 0
                        NavigationDrawerItem(
                            label = { Text(item.screen.title + if (unread > 0) "  ($unread)" else "") },
                            icon = { Icon(item.icon, null) }, selected = vm.screen == item.screen,
                            onClick = { scope.launch { drawer.close() }; vm.go(item.screen) },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    NavigationDrawerItem(label = { Text("Ana Menüye Dön") }, icon = { Icon(Icons.Filled.Close, null) }, selected = false,
                        onClick = { scope.launch { drawer.close() }; vm.quitToMenu() }, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = Bg,
            topBar = {
                if (inGame && gs != null) TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchDark),
                    navigationIcon = {
                        if (vm.screen in navItems.map { it.screen }) IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Filled.Menu, "Menü", tint = Color.White) }
                        else IconButton(onClick = { vm.back() }) { Icon(Icons.Filled.ArrowBack, "Geri", tint = Color.White) }
                    },
                    title = {
                        Column {
                            Text(vm.screen.title, fontSize = 17.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${Scheduler.dateStr(gs.season, gs.day)} · ${Economy.fmt(gs.userClub.finances.balance)}", fontSize = 11.sp, color = Gold, maxLines = 1)
                        }
                    },
                    actions = {
                        if (vm.screen != Screen.PRE_MATCH) Button(onClick = { vm.advance() }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.padding(end = 6.dp)) {
                            Icon(Icons.Filled.PlayArrow, null); Text("DEVAM", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (vm.screen) {
                    Screen.MENU -> MenuScreen(vm)
                    Screen.CLUB_SELECT -> ClubSelectScreen(vm)
                    Screen.HOME -> HomeScreen(vm)
                    Screen.INBOX -> InboxScreen(vm)
                    Screen.SQUAD -> SquadScreen(vm)
                    Screen.TACTICS -> TacticsScreen(vm)
                    Screen.TRAINING -> TrainingScreen(vm)
                    Screen.TRANSFERS -> TransferScreen(vm)
                    Screen.FIXTURES -> FixturesScreen(vm)
                    Screen.TABLES -> TablesScreen(vm)
                    Screen.COMPETITIONS -> CompetitionsScreen(vm)
                    Screen.YOUTH -> YouthScreen(vm)
                    Screen.FACILITIES -> FacilitiesScreen(vm)
                    Screen.FINANCE -> FinanceScreen(vm)
                    Screen.CLUB -> ClubScreen(vm)
                    Screen.NATIONAL -> NationalScreen(vm)
                    Screen.PRE_MATCH -> PreMatchScreen(vm)
                    Screen.LIVE_MATCH -> LiveMatchScreen(vm)
                    Screen.MATCH_REPORT -> MatchReportScreen(vm)
                    Screen.PLAYER -> PlayerScreen(vm)
                    Screen.CLUB_VIEW -> ClubViewScreen(vm)
                    Screen.SAVE_LOAD -> SaveLoadScreen(vm)
                    Screen.SACKED -> SackedScreen(vm)
                }
            }
        }
    }
    // basın toplantısı
    val press = gs?.pendingPress
    if (press != null && inGame) PressDialog(press) { vm.answerPress(it) }
}

@Composable
private fun BusyScreen(vm: GameViewModel) {
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("⚽", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            if (vm.progress > 0f) LinearProgressIndicator(progress = { vm.progress }, modifier = Modifier.fillMaxWidth())
            else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(vm.busyText, color = Muted)
        }
    }
}

@Composable
private fun PressDialog(pc: com.futbolmenajer.data.model.PressConference, onAnswer: (com.futbolmenajer.data.model.PressOption) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Filled.Mic, null) },
        title = { Text(pc.context, fontSize = 16.sp) },
        text = {
            Column {
                Text("Gazeteci: \"${pc.question}\"", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                pc.options.forEach { o ->
                    OutlinedButton(onClick = { onAnswer(o) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(o.text, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
