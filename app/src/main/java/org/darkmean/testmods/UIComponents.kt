package org.darkmean.testmods

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material.icons.outlined.Inbox
import java.io.File
import androidx.core.net.toUri

// ─────────────────────────────────────────────────────────────────────
// HTML → AnnotatedString
// ─────────────────────────────────────────────────────────────────────
fun htmlToAnnotatedString(html: String): AnnotatedString {
    return runCatching {
        val normalized = html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<p\\s*/?>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")

        val spanned =
            Html.fromHtml(normalized, Html.FROM_HTML_MODE_COMPACT)

        val rawText = spanned.toString().trimEnd()

        buildAnnotatedString {
            append(rawText)
            for (span in spanned.getSpans(0, spanned.length, android.text.style.StyleSpan::class.java)) {
                val s = spanned.getSpanStart(span); val e = spanned.getSpanEnd(span)
                when (span.style) {
                    android.graphics.Typeface.BOLD ->
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, e)
                    android.graphics.Typeface.ITALIC ->
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), s, e)
                    android.graphics.Typeface.BOLD_ITALIC ->
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), s, e)
                }
            }
            for (span in spanned.getSpans(0, spanned.length, android.text.style.UnderlineSpan::class.java)) {
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline),
                    spanned.getSpanStart(span), spanned.getSpanEnd(span))
            }
            for (span in spanned.getSpans(0, spanned.length, android.text.style.StrikethroughSpan::class.java)) {
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough),
                    spanned.getSpanStart(span), spanned.getSpanEnd(span))
            }
            for (span in spanned.getSpans(0, spanned.length, android.text.style.ForegroundColorSpan::class.java)) {
                val argb = span.foregroundColor
                val r = (argb shr 16) and 0xFF; val g = (argb shr 8) and 0xFF; val b = argb and 0xFF
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (luminance < 60) continue
                addStyle(SpanStyle(color = Color(argb)),
                    spanned.getSpanStart(span), spanned.getSpanEnd(span))
            }
            for (span in spanned.getSpans(0, spanned.length, android.text.style.URLSpan::class.java)) {
                val s = spanned.getSpanStart(span); val e = spanned.getSpanEnd(span)
                addStyle(SpanStyle(color = ThemePrimary, textDecoration = TextDecoration.Underline), s, e)
                addStringAnnotation("URL", span.url, s, e)
            }
            for (span in spanned.getSpans(0, spanned.length, android.text.style.RelativeSizeSpan::class.java)) {
                val s = spanned.getSpanStart(span); val e = spanned.getSpanEnd(span)
                addStyle(SpanStyle(fontSize = (13 * span.sizeChange).sp), s, e)
            }
            for (span in spanned.getSpans(0, spanned.length, android.text.style.TypefaceSpan::class.java)) {
                if (span.family == "monospace") {
                    addStyle(SpanStyle(fontFamily = FontFamily.Monospace),
                        spanned.getSpanStart(span), spanned.getSpanEnd(span))
                }
            }
        }
    }.getOrElse { AnnotatedString(html) }
}

@Composable
fun ClickableHtmlText(
    annotated: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    color: Color = ColorTextSecondary,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val context = LocalContext.current
    androidx.compose.foundation.text.ClickableText(
        text     = annotated,
        modifier = modifier,
        style    = androidx.compose.ui.text.TextStyle(fontSize = fontSize, color = color, lineHeight = lineHeight),
        maxLines = maxLines,
        overflow = overflow,
        onClick  = { offset ->
            annotated.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { annotation ->
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                        annotation.item.toUri())) }
                }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(strings: AppStrings, modCount: Int, onRefresh: () -> Unit, onSettings: () -> Unit, onLibrary: () -> Unit) {
    Surface(
        color    = ColorBgCard,
        modifier = Modifier.border(0.5.dp, ThemePrimary.copy(alpha = 0.25f), RoundedCornerShape(0.dp))
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(strings.appTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorTextPrimary)
                    Text("$modCount ${strings.modsCountSuffix}", fontSize = 11.sp, color = ThemePrimary)
                }
            },
            colors  = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                IconButton(onClick = onLibrary) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = strings.libraryTitle, tint = ThemePrimary)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = strings.settingsTitle, tint = ThemePrimary)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ThemePrimary)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// BOTTOM BAR — 2 install buttons + launch game
// ─────────────────────────────────────────────────────────────────────
@Composable
fun AppBottomBar(
    strings:           AppStrings,
    scope:             kotlinx.coroutines.CoroutineScope,
    context:           Context,
    onPickFileForNulls: () -> Unit,   // "Install in Null's Brawl" — downloads then fires Intent
    onPickFileToFolder: () -> Unit    // "Install in folder" — saves to modsDir
) {
    Surface(
        color    = ColorBgCard,
        modifier = Modifier.border(0.5.dp, ColorBgStroke, RoundedCornerShape(0.dp))
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Button 1: Install in Null's Brawl
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(ThemeDark, ThemePrimary, ThemeAccent)))
                    .clickable { onPickFileForNulls() },
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.InstallMobile, null, tint = ThemeOnPrimary, modifier = Modifier.size(20.dp))
                    Text(strings.installInNullsBrawl, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = ThemeOnPrimary)
                }
            }

            // Button 2: Install in folder
            OutlinedButton(
                onClick  = { onPickFileToFolder() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = ThemePrimary),
                border   = androidx.compose.foundation.BorderStroke(1.dp, ThemeDark)
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.installInFolder, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }

            // Launch game
            OutlinedButton(
                onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.GAME_SCHEME))) }
                    catch (e: Exception) { android.widget.Toast.makeText(context, strings.gameNotFound, android.widget.Toast.LENGTH_SHORT).show() }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = ThemePrimary),
                border   = androidx.compose.foundation.BorderStroke(1.dp, ThemeDark)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.launchGameButton, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// WARNING CARD
// ─────────────────────────────────────────────────────────────────────
@Composable
fun WarningCard(strings: AppStrings, context: Context, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "border_alpha"
    )
    Card(
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF1A1500)),
        shape    = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF3D3000).copy(alpha = borderAlpha), RoundedCornerShape(14.dp))
            .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("nullsbrawlmods://open"))) } }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2A2000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFD600), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(strings.warningTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFFD600))
            }
            Spacer(Modifier.height(10.dp))
            Text(strings.warningBody, fontSize = 12.sp, color = Color(0xFFBBAA00), lineHeight = 17.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Info, null, tint = Color(0xFFFFAA00), modifier = Modifier.size(12.dp))
                Text(strings.warningBodyNullsVersion, fontSize = 11.sp, color = Color(0xFFFFAA00), lineHeight = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(Icons.Default.Close, null, tint = Color(0xFF888866), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.warningDismiss, fontSize = 11.sp, color = Color(0xFF888866))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// MOD CARD
// ─────────────────────────────────────────────────────────────────────
@Composable
fun ModCard(mod: ModItem, strings: AppStrings, modifier: Modifier = Modifier, onClick: () -> Unit = {}, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val iconBitmap = remember(mod.path) {
        runCatching {
            val file = File(mod.path, "icon.png")
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
        }.getOrNull()
    }
    var sigInfo by remember { mutableStateOf<ModSignatureInfo?>(null) }
    LaunchedEffect(mod.path) {
        if (!mod.isBroken) {
            sigInfo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { checkModSignature(File(mod.path)) }
        }
    }
    val descAnnotated  = remember(mod.desc)   { if (mod.desc.contains('<')   && mod.desc.contains('>')  ) htmlToAnnotatedString(mod.desc)   else AnnotatedString(mod.desc)   }
    val titleAnnotated = remember(mod.title)  { if (mod.title.contains('<')  && mod.title.contains('>') ) htmlToAnnotatedString(mod.title)  else AnnotatedString(mod.title)  }
    val authorAnnotated= remember(mod.author) { if (mod.author.contains('<') && mod.author.contains('>')) htmlToAnnotatedString(mod.author) else AnnotatedString(mod.author) }

    val cardBorder = if (mod.isBroken) ColorError.copy(alpha = 0.5f) else ThemeCardTint.copy(alpha = 0.5f)
    val cardBg     = if (mod.isBroken) Color(0xFF1A0000) else ColorBgCard

    Card(
        colors   = CardDefaults.cardColors(containerColor = cardBg),
        shape    = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
            .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable { if (!mod.isBroken) onClick() }
    ) {
        Column {
            if (!mod.isBroken) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp)
                    .background(Brush.horizontalGradient(listOf(ThemeDark, ThemePrimary, ThemeAccent.copy(alpha = 0.5f)))))
            }
            if (mod.isBroken) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(ColorError.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.BrokenImage, null, tint = ColorError, modifier = Modifier.size(14.dp))
                    Text(strings.modBroken, fontSize = 11.sp, color = ColorError, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(if (mod.isBroken) listOf(Color(0xFF2A0000), Color(0xFF1A0000)) else ThemeAvatarBg))
                        .border(0.5.dp, if (mod.isBroken) ColorError.copy(0.3f) else ThemePrimary.copy(0.2f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        Image(iconBitmap, mod.title, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp)))
                    } else {
                        Icon(
                            if (mod.isBroken) Icons.Default.BrokenImage else Icons.Default.Extension,
                            null, tint = if (mod.isBroken) ColorError else ThemePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (mod.isBroken) {
                        Text(mod.id.take(24), fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            color = ColorError, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text(titleAnnotated, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = ColorTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    }
                    if (!mod.isBroken && mod.desc.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        ClickableHtmlText(annotated = descAnnotated, fontSize = 12.sp, color = ColorTextSecondary,
                            maxLines = 2, lineHeight = 17.sp, overflow = TextOverflow.Ellipsis)
                    }
                    if (!mod.isBroken && mod.author.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Person, null, tint = ColorTextMuted, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(authorAnnotated, fontSize = 11.sp, color = ColorTextMuted,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false).widthIn(max = 140.dp))
                            if (mod.version.isNotBlank()) {
                                Spacer(Modifier.width(4.dp))
                                Text("v${mod.version}".take(12), fontSize = 10.sp, color = ColorTextMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    sigInfo?.let { sig ->
                        Spacer(Modifier.height(4.dp))
                        SignatureBadge(sig, strings)
                    }
                    if (!mod.isBroken) {
                        Spacer(Modifier.height(3.dp))
                        Text("ID: ${mod.id.take(8)}…", fontSize = 10.sp, color = Color(0xFF303030))
                    }
                }
                AnimatedContent(targetState = showConfirm, label = "delete_confirm") { confirmed ->
                    if (!confirmed) {
                        IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.Default.Delete, strings.deleteLabel, tint = ColorTextMuted, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Row {
                            IconButton(onClick = { showConfirm = false }, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Default.Close, null, tint = ColorTextSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { showConfirm = false; onDelete() }, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Default.Delete, strings.deleteLabel, tint = ColorError, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignatureBadge(info: ModSignatureInfo, strings: AppStrings) {
    val (text, color) = when (info.status) {
        SignatureStatus.NOT_SIGNED   -> Pair(strings.sigNotSigned, ColorError)
        SignatureStatus.EXPIRES_SOON -> Pair(strings.sigExpiresSoon.format(info.daysLeft),
            if (info.daysLeft < 14) Color(0xFFFFAA00) else Color(0xFF69F0AE))
        SignatureStatus.EXPIRED      -> Pair(strings.sigExpired, Color(0xFFFFAA00))
        SignatureStatus.NOT_ORIGINAL -> Pair(strings.sigNotOriginal, ColorError)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val icon = when (info.status) {
            SignatureStatus.NOT_SIGNED, SignatureStatus.NOT_ORIGINAL -> Icons.Default.GppBad
            SignatureStatus.EXPIRED     -> Icons.Default.GppMaybe
            SignatureStatus.EXPIRES_SOON -> Icons.Default.GppGood
        }
        Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EmptyState(strings: AppStrings) {
    Box(Modifier.fillMaxSize().padding(top = 80.dp), Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier         = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(ColorBgCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Inbox, null, tint = ColorTextMuted, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(strings.emptyListTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ColorTextSecondary)
            Spacer(Modifier.height(6.dp))
            Text(strings.emptyListSubtitle, fontSize = 13.sp, color = ColorTextMuted)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// BAN DIALOG
// ─────────────────────────────────────────────────────────────────────
@Composable
fun BanDialog(deviceId: String, reason: String, strings: AppStrings, onDismiss: () -> Unit) {
    val reasonAnnotated = remember(reason) {
        if (reason.isNotBlank() && reason.contains('<') && reason.contains('>'))
            htmlToAnnotatedString(reason)
        else AnnotatedString(reason)
    }
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { },
        containerColor   = ColorBgCard,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Block, null, tint = ColorError)
                Spacer(Modifier.width(8.dp))
                Text(strings.banTitle, color = ColorError, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(strings.banMessage, color = ColorTextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(strings.banDetails, color = ColorTextSecondary)
                if (reason.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(color = Color(0xFF1A0000), shape = RoundedCornerShape(10.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = ColorError, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.banReasonLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ColorError)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(text = reasonAnnotated, fontSize = 13.sp, color = ColorTextPrimary, lineHeight = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(color = ColorBgDeep, shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(deviceId))
                        copied = true
                    }) {
                    Row(modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Device ID: $deviceId", fontSize = 11.sp, color = if (copied) ThemePrimary else ColorTextMuted,
                            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(6.dp))
                        Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy", tint = if (copied) ThemePrimary else ColorTextMuted,
                            modifier = Modifier.size(14.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ColorError),
                shape = RoundedCornerShape(10.dp)) {
                Text(strings.banButton, fontWeight = FontWeight.SemiBold)
            }
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

// ─────────────────────────────────────────────────────────────────────
// BROKEN FILE DIALOG
// ─────────────────────────────────────────────────────────────────────
@Composable
fun BrokenFileDialog(strings: AppStrings, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = ColorBgCard,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2A0000)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BrokenImage, null, tint = ColorError, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(strings.brokenFileTitle, color = ColorError, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(strings.brokenFileError, color = ColorTextPrimary, fontSize = 14.sp)
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(color = ColorBgDeep, shape = RoundedCornerShape(8.dp)) {
                        Text(message, modifier = Modifier.padding(10.dp), fontSize = 11.sp,
                            color = ColorTextSecondary, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ColorBgStroke),
                shape = RoundedCornerShape(10.dp)) {
                Text(strings.banButton, fontWeight = FontWeight.SemiBold, color = ColorTextPrimary)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────
// SETTINGS HELPERS
// ─────────────────────────────────────────────────────────────────────
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title.uppercase(), fontSize = 11.sp, color = ThemePrimary, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = ColorBgCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, ColorBgStroke, RoundedCornerShape(14.dp))) {
            Column(Modifier.padding(12.dp), content = content)
        }
    }
}

@Composable
fun SettingsLinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, url: String, context: Context) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } }
        .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = ThemePrimary, modifier = Modifier.size(20.dp))
        Text(label, color = ColorTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.OpenInNew, null, tint = ColorTextMuted, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = ColorTextSecondary, modifier = Modifier.weight(0.4f))
        Text(value, fontSize = 12.sp, color = ColorTextPrimary, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun FileTreeRow(entry: FileEntry) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = (entry.depth * 14).dp).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(if (entry.isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            null, tint = if (entry.isDir) ThemePrimary else ColorTextSecondary,
            modifier = Modifier.size(14.dp))
        Text(entry.name, fontSize = 12.sp, color = if (entry.isDir) ColorTextPrimary else ColorTextSecondary,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!entry.isDir && entry.sizeMb > 0)
            Text("%.1f MB".format(entry.sizeMb), fontSize = 10.sp, color = ColorTextMuted)
    }
}

// ─────────────────────────────────────────────────────────────────────
// JSON TREE
// ─────────────────────────────────────────────────────────────────────
@Composable
fun JsonTreeNode(key: String, value: Any?, depth: Int = 0) {
    val indent = (depth * 12).dp
    when (value) {
        is org.json.JSONObject -> {
            var expanded by remember { mutableStateOf(depth < 2) }
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(start = indent).clip(RoundedCornerShape(4.dp))
                    .clickable { expanded = !expanded }.padding(vertical = 3.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        null, tint = ThemePrimary, modifier = Modifier.size(14.dp))
                    Text(if (key.isNotBlank()) "\"$key\":" else "{…}",
                        fontSize = 11.sp, color = ThemePrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                    if (!expanded) Text("{${value.length()}}", fontSize = 10.sp, color = ColorTextMuted, fontFamily = FontFamily.Monospace)
                }
                if (expanded) value.keys().asSequence().toList().forEach { k -> JsonTreeNode(k, value.opt(k), depth + 1) }
            }
        }
        is org.json.JSONArray -> {
            var expanded by remember { mutableStateOf(depth < 1) }
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(start = indent).clip(RoundedCornerShape(4.dp))
                    .clickable { expanded = !expanded }.padding(vertical = 3.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        null, tint = ThemeAccent, modifier = Modifier.size(14.dp))
                    Text(if (key.isNotBlank()) "\"$key\":" else "[…]",
                        fontSize = 11.sp, color = ThemeAccent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                    Text("[${value.length()}]", fontSize = 10.sp, color = ColorTextMuted, fontFamily = FontFamily.Monospace)
                }
                if (expanded) for (i in 0 until value.length()) JsonTreeNode("[$i]", value.opt(i), depth + 1)
            }
        }
        else -> {
            val strVal = value?.toString() ?: "null"
            val valColor = when {
                value == null           -> Color(0xFF888888)
                value is Boolean        -> Color(0xFF569CD6)
                value is Number         -> Color(0xFFB5CEA8)
                strVal.startsWith("http") -> ThemePrimary
                else                    -> Color(0xFFCE9178)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(start = indent).padding(vertical = 2.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (key.isNotBlank()) {
                    Text("\"$key\":", fontSize = 10.sp, color = ColorTextSecondary,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.widthIn(max = 120.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(if (value is String) "\"$strVal\"" else strVal,
                    fontSize = 10.sp, color = valColor, fontFamily = FontFamily.Monospace,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
    }
}