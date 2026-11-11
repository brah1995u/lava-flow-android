package com.lavaflow.puzzle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.*
import com.lavaflow.puzzle.*
import com.lavaflow.puzzle.R
import com.lavaflow.puzzle.core.*
import kotlinx.coroutines.delay

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LavaShell(state: ShellState, model: ShellViewModel, feedback: FeedbackController) {
    MaterialTheme {
        val nav = rememberNavController()
        val route by nav.currentBackStackEntryAsState()
        val premium = (route?.destination?.route ?: "home") in setOf("home", "gameplay")
        val introFinished = rememberLaunchIntroFinished()
        val startupVisible = state.profile == null || !introFinished
        // The established Home art remains separate and cached: full volcanic background, title and source.
        val titleArtwork = painterResource(R.drawable.vr_title)
        val volcanoArtwork = painterResource(R.drawable.vr_volcano)
        val rewardDay = rememberRewardDay()
        val atmosphereClock = if (premium || startupVisible) rememberForegroundClock() else 0f
        Box(Modifier.fillMaxSize().background(Deep).semantics { testTagsAsResourceId = true }) {
            Image(painterResource(if (premium) R.drawable.vr_background else R.drawable.bg_volcanic_stone), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Deep.copy(alpha = if (premium) .10f else .30f), Color.Transparent, Deep.copy(alpha = if (premium) .36f else .80f)))))
            if (premium && !startupVisible) EmberField(Modifier.fillMaxSize(), atmosphereClock)
            val profile = state.profile
            // Do not render Home behind the real startup presentation. Otherwise the layered
            // masthead briefly duplicates while the 900 ms foreground intro is still visible.
            if (profile != null && !startupVisible) {

                fun go(route: String) { feedback.click(); model.dismissNotice(); nav.navigate(route) { launchSingleTop = true } }
                val back: () -> Unit = { feedback.click(); model.dismissNotice(); nav.popBackStack() }
                val home: () -> Unit = { feedback.click(); model.dismissNotice(); nav.popBackStack("home", false) }
                CompositionLocalProvider(LocalHomeAction provides home) {
                NavHost(navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize().safeDrawingPadding()
                    .then(if (startupVisible) Modifier.clearAndSetSemantics { } else Modifier),
                    enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) }) {
                    composable("home") { PremiumHome(profile, ::go, titleArtwork, volcanoArtwork, rewardDay) }
                    composable("daily") { DailyRewardsScreen(profile, rewardDay, state.busy, back) {
                        model.claimDaily { feedback.click(purchase = true) }
                    } }
                    composable("levels") { LevelSelect(profile, back) { level -> model.openLevel(level) { go("gameplay") } } }
                    composable("gameplay") {
                        GameplayScreen(state, model, feedback,
                            home = { nav.popBackStack("home", false) },
                            levels = { nav.popBackStack("levels", false) },
                            settings = { go("settings") }, shop = { go("shop") })
                    }
                    composable("shop") { Shop(profile, state.busy, back) { model.buy(it) { feedback.click(purchase = true) } } }
                    composable("achievements") { Achievements(profile, back) }
                    composable("leaderboard") { Leaderboard(profile, back) }
                    composable("settings") { Settings(profile, back) { setting, enabled -> feedback.click(); model.setting(setting, enabled) } }
                    composable("about") { About(profile, back) }
                    composable("privacy") { DocumentScreen("PRIVACY", privacySections, profile, back) }
                    composable("legal") { Legal(profile, back) }
                }
                }
                if (state.notice != null) {
                    LaunchedEffect(state.notice) { delay(4000); model.dismissNotice() }
                    Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp)
                        .background(Purple, RoundedCornerShape(18.dp)).border(1.dp, Gold, RoundedCornerShape(18.dp))
                        .clickable { model.dismissNotice() }.padding(18.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(state.notice, color = Cream, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
                if (state.achievementNotice != null) {
                    LaunchedEffect(state.achievementNotice) { feedback.play(FeedbackEvent.ACHIEVEMENT); delay(4200); model.dismissAchievement() }
                    RelicPanel(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp).fillMaxWidth()
                        .clickable { model.dismissAchievement() }, edge = 12.dp) {
                        Column(Modifier.padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ACHIEVEMENT UNLOCKED", color = Gold, fontSize = 10.sp, letterSpacing = 1.sp)
                            Text(state.achievementNotice, color = Cream, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            StartupPresentation(startupVisible, profile != null, state.error, model::load, titleArtwork, volcanoArtwork, atmosphereClock)
        }
    }
}

@Composable
internal fun PageHeader(title: String, profile: PlayerProfile, back: () -> Unit) {
    val home = LocalHomeAction.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconAction("Back", R.drawable.orn_back, "back", onClick = back)
            RelicWallet(profile.coins)
            IconAction("Home", R.drawable.ic_home_gold, "page_home", onClick = home)
        }
        ForgeFrame(Modifier.fillMaxWidth().padding(top = 5.dp).heightIn(min = 65.dp), 31.dp) {
            Text(title, style = TitleStyle, fontSize = 23.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 19.dp))
        }
    }
}

@Composable
private fun LevelSelect(profile: PlayerProfile, back: () -> Unit, play: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("CHOOSE YOUR PATH", profile, back)
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${profile.unlockedLevel} / ${GameCatalog.LEVEL_SLOTS} UNLOCKED", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp)
            Text("${profile.totalStars} STARS", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LazyVerticalGrid(GridCells.Fixed(5), modifier = Modifier.weight(1f).testTag("level_grid"),
            contentPadding = PaddingValues(18.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            items((1..GameCatalog.LEVEL_SLOTS).toList(), key = { it }) { level ->
                val unlocked = level <= profile.unlockedLevel && level in GameCatalog.playableLevels
                Column(Modifier.testTag("level_$level").semantics(mergeDescendants = true) {
                    contentDescription = "Level $level, ${if (!unlocked) "locked" else if (level in profile.bestStars) "completed, ${profile.bestStars[level]} stars" else "unlocked"}"
                }.clickable(enabled = unlocked, role = Role.Button, onClick = { play(level) }), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).alpha(if (unlocked) 1f else .35f), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ui_button_gold_octagonal), null, Modifier.fillMaxSize())
                        if (unlocked) Text(level.toString(), color = Color(0xFF482616), fontSize = 25.sp, fontWeight = FontWeight.Black)
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LockGlyph(Modifier.size(15.dp), Color(0xFF49251B))
                            Text(level.toString(), color = Color(0xFF482616), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    StarRow(profile.bestStars[level] ?: 0, 12.dp)
                    Text("${RewardEconomy.levelCoins(level)}+", color = Gold, fontSize = 10.sp,
                        modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Text("Higher levels bring bigger rewards.\nSave moves to earn more coins on every clear.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(18.dp))
    }
}

@Composable
private fun Shop(profile: PlayerProfile, busy: Boolean, back: () -> Unit, buy: (Booster) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("THE BOOSTER SHOP", profile, back)
        Text("A little help. A brilliant solution.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp))
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(17.dp), modifier = Modifier.testTag("shop_list")) {
            items(Booster.entries, key = { it.name }) { booster ->
                val purchaseFlash = remember { Animatable(1f) }
                var previousStock by remember { mutableIntStateOf(profile.stock(booster)) }
                LaunchedEffect(profile.stock(booster)) {
                    if (profile.stock(booster) > previousStock) { purchaseFlash.snapTo(1.10f); purchaseFlash.animateTo(1f, tween(450)) }
                    previousStock = profile.stock(booster)
                }
                ArtPanel(booster.title, resource = R.drawable.ui_panel_purple_compact) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(boosterArt(booster)), null, Modifier.size(78.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("YOUR COLLECTION", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                        Text("OWNED  ${profile.stock(booster)}", color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("stock_${booster.name.lowercase()}").graphicsLayer { scaleX = purchaseFlash.value; scaleY = purchaseFlash.value })
                        }
                    }
                    Text(booster.description, color = Cream, fontSize = 15.sp, lineHeight = 21.sp)
                    GoldAction("BUY  ·  ${booster.price} COINS", { buy(booster) }, Modifier.fillMaxWidth(),
                        enabled = !busy && profile.coins >= booster.price && profile.stock(booster) < Int.MAX_VALUE, tag = "buy_${booster.name.lowercase()}")
                    if (profile.coins < booster.price) Text("More coins needed", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            item { Text("Boosters stay in your collection until you use them.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
        }
    }
}

@Composable
private fun Achievements(profile: PlayerProfile, back: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("MARKS OF MASTERY", profile, back)
        Text("${profile.unlockedAchievements.size} / ${Achievement.entries.size} ACHIEVEMENTS", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.testTag("achievement_list")) {
            items(Achievement.entries, key = { it.name }) { achievement ->
                val unlocked = achievement.name in profile.unlockedAchievements
                val progress = achievement.progress(profile)
                ForgeFrame(Modifier.fillMaxWidth().testTag("achievement_${achievement.name.lowercase()}"), 37.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 23.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Image(painterResource(achievementArt(achievement)), null,
                            Modifier.size(68.dp).alpha(if (unlocked) 1f else .46f))
                        Column(Modifier.weight(1f)) {
                        Text(achievement.title, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 5.dp))
                        Text(achievement.description, color = Cream, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (unlocked) "UNLOCKED" else "LOCKED", color = if (unlocked) Gold else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("$progress / ${achievement.target}", color = Gold, fontSize = 12.sp)
                    }
                    Box(Modifier.fillMaxWidth().height(6.dp).background(Deep, CircleShape)) {
                        if (progress > 0) Box(Modifier.fillMaxWidth(progress.toFloat() / achievement.target).fillMaxHeight().background(Brush.horizontalGradient(listOf(Lava, Gold)), CircleShape))
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun Settings(profile: PlayerProfile, back: () -> Unit, update: (Setting, Boolean) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("MAKE IT YOURS", profile, back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            ArtPanel("SOUND & FEEL") {
                SettingRow("Music", "The warmth of the volcano", R.drawable.orn_music, profile.settings.music, "setting_music") { update(Setting.MUSIC, it) }
                DividerLine()
                SettingRow("Sound Effects", "Every tap, a little spark", R.drawable.ic_sound_speaker, profile.settings.sound, "setting_sound") { update(Setting.SOUND, it) }
                DividerLine()
                SettingRow("Vibration", "Feel your next move", R.drawable.orn_vibration, profile.settings.vibration, "setting_vibration") { update(Setting.VIBRATION, it) }
            }
            Text("Your preferences are saved automatically.", color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, icon: Int, enabled: Boolean, tag: String, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).toggleable(enabled, role = Role.Switch, onValueChange = change)
        .semantics { stateDescription = if (enabled) "On" else "Off" }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Image(painterResource(icon), null, Modifier.size(42.dp).alpha(if (enabled) 1f else .4f))
        Column(Modifier.weight(1f)) {
            Text(title, color = Cream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(58.dp).height(32.dp).background(if (enabled) Color(0xFF9F5C20) else Deep, CircleShape).border(1.dp, Gold.copy(alpha = .8f), CircleShape).padding(4.dp)) {
                Box(Modifier.align(if (enabled) Alignment.CenterEnd else Alignment.CenterStart).size(24.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFEBA0), Color(0xFFC18224))), CircleShape))
            }
            Text(if (enabled) "ON" else "OFF", color = if (enabled) Gold else Muted, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun About(profile: PlayerProfile, back: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("ABOUT LAVA FLOW", profile, back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ArtPanel("FIND YOUR FLOW") {
                Image(painterResource(R.drawable.ic_coin_volcano), null, Modifier.size(76.dp).align(Alignment.CenterHorizontally))
                Text("A little patience. A spark of ingenuity.", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Lava Flow is a puzzle journey through volcanic stone. Rotate channels, discover connections, and guide the lava home.", color = Cream, fontSize = 15.sp, lineHeight = 23.sp)
                Text("VERSION ${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

private val privacySections = listOf(
    "YOUR PROGRESS" to "Your coin balance, booster collection, level progress, achievements and preferences are stored locally on this device.",
    "OFFLINE BY DESIGN" to "This version has no accounts, advertising, analytics or online services. It does not request access to your contacts, photos or location.",
    "YOUR DEVICE" to "Music and sound use the device audio system. Vibration is used only when enabled in your preferences. Feedback pauses when the app is in the background.",
    "MANAGING YOUR DATA" to "You can remove the local save through Android's app storage controls or by uninstalling the app. This removes your progress and collection. This version does not provide cloud backup."
)

@Composable
private fun Legal(profile: PlayerProfile, back: () -> Unit) {
    val context = LocalContext.current
    var notices by remember { mutableStateOf<String?>(null) }
    var showNotices by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { notices = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { context.assets.open("open_source_notices.txt").bufferedReader().use { it.readText() } } }
    if (showNotices) {
        BackHandler { showNotices = false }
        Column(Modifier.fillMaxSize()) {
            PageHeader("SOFTWARE NOTICES", profile) { showNotices = false }
            ArtPanel("OPEN SOURCE", Modifier.weight(1f).padding(18.dp)) {
                Text(notices ?: "Loading notices…", color = Cream, fontSize = 14.sp, lineHeight = 23.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        }
    } else Column(Modifier.fillMaxSize()) {
        PageHeader("LEGAL & TERMS", profile, back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ArtPanel("GAME CONTENT") {
                Text("Lava Flow is a game for personal entertainment. Coins and boosters are virtual game items. They have no cash value and cannot be redeemed for money.", color = Cream, fontSize = 15.sp, lineHeight = 23.sp)
            }
            ArtPanel("LOCAL EXPERIENCE") {
                Text("Progress is stored on your device. Deleting the app's data removes that local progress. This version has no real-money purchases or online player services.", color = Cream, fontSize = 15.sp, lineHeight = 23.sp)
            }
            GoldAction("OPEN-SOURCE NOTICES", { showNotices = true }, Modifier.fillMaxWidth(), enabled = notices != null, tag = "notices")
        }
    }
}

@Composable
private fun DocumentScreen(title: String, sections: List<Pair<String, String>>, profile: PlayerProfile, back: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader(title, profile, back)
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(sections) { (heading, text) -> ArtPanel(heading) { Text(text, color = Cream, fontSize = 14.sp, lineHeight = 23.sp) } }
        }
    }
}

@Composable
private fun ThemedDialog(title: String, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(dismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(20.dp).widthIn(max = 430.dp).verticalScroll(rememberScrollState())) {
            ArtPanel(title) { content() }
        }
    }
}

@Composable private fun DividerLine() { Box(Modifier.fillMaxWidth().height(1.dp).background(Gold.copy(alpha = .20f))) }

@Composable
private fun LockGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        drawArc(color, 180f, 180f, false, Offset(size.width * .25f, 0f), Size(size.width * .5f, size.height * .65f), style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * .13f))
        drawRoundRect(color, Offset(size.width * .12f, size.height * .38f), Size(size.width * .76f, size.height * .6f), CornerRadius(size.width * .10f))
    }
}


