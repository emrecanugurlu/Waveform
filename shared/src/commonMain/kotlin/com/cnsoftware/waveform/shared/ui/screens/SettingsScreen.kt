@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cnsoftware.waveform.shared.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cnsoftware.waveform.shared.ui.ThemeMode
import com.cnsoftware.waveform.shared.ui.ThemePalette
import com.cnsoftware.waveform.shared.ui.appVersionName
import com.cnsoftware.waveform.shared.ui.components.ConnectedToggleRow
import com.cnsoftware.waveform.shared.ui.components.SettingsActionRow
import com.cnsoftware.waveform.shared.ui.components.SettingsSectionTitle
import com.cnsoftware.waveform.shared.ui.components.SettingsSwitchRow
import com.cnsoftware.waveform.shared.ui.displayName
import com.cnsoftware.waveform.shared.ui.isDark
import com.cnsoftware.waveform.shared.ui.previewColors

@Suppress("DEPRECATION")
@Composable
internal fun SettingsScreen(
    autoPlayNext: Boolean,
    onSetAutoPlayNext: (Boolean) -> Unit,
    skipBackSeconds: Int,
    onSetSkipBackSeconds: (Int) -> Unit,
    skipForwardSeconds: Int,
    onSetSkipForwardSeconds: (Int) -> Unit,
    isBackgroundRefreshSupported: Boolean,
    backgroundRefreshEnabled: Boolean,
    onSetBackgroundRefresh: (Boolean) -> Unit,
    systemNotificationsEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    downloadedCount: Int,
    onDeleteAllDownloads: () -> Unit,
    hasListeningHistory: Boolean,
    onClearListeningHistory: () -> Unit,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    themePalette: ThemePalette,
    onSetThemePalette: (ThemePalette) -> Unit,
    subscriptionsCount: Int,
    isImportingOpml: Boolean,
    onExportOpml: () -> String,
    onImportOpml: (String) -> Unit
) {
    var confirmDeleteDownloads by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var exportedOpml by remember { mutableStateOf<String?>(null) }
    var opmlCopied by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importInput by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    val versionName = remember { appVersionName() }

    val darkTheme = themeMode.isDark(isSystemInDarkTheme())
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SettingsSectionTitle("Görünüm")
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "Tema Modu", style = MaterialTheme.typography.titleSmall)
                ConnectedToggleRow(
                    items = listOf(
                        ThemeMode.SYSTEM to "Sistem",
                        ThemeMode.LIGHT to "Açık",
                        ThemeMode.DARK to "Koyu"
                    ),
                    selected = themeMode,
                    onSelect = onSetThemeMode
                )

                Text(text = "Renk Paleti", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemePalette.entries.forEach { palette ->
                        val selected = palette == themePalette
                        val (primarySwatch, tertiarySwatch) = palette.previewColors(darkTheme)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetThemePalette(palette) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = if (selected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(MaterialShapes.Cookie9Sided.toShape())
                                        .background(primarySwatch),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(MaterialShapes.Circle.toShape())
                                            .background(tertiarySwatch)
                                    )
                                    if (selected) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = palette.displayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsSectionTitle("Oynatma")
        SettingsCard {
            Column {
                SettingsSwitchRow(
                    title = "Otomatik sonraki bölüm",
                    description = "Bölüm bitince sıradaki bölüm veya daha yeni bölüm kendiliğinden başlar.",
                    checked = autoPlayNext,
                    onCheckedChange = onSetAutoPlayNext
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(text = "Geri sarma süresi", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ConnectedToggleRow(
                        items = listOf(5 to "5 sn", 10 to "10 sn", 15 to "15 sn", 30 to "30 sn"),
                        selected = skipBackSeconds,
                        onSelect = onSetSkipBackSeconds
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "İleri sarma süresi", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ConnectedToggleRow(
                        items = listOf(15 to "15 sn", 30 to "30 sn", 45 to "45 sn", 60 to "60 sn"),
                        selected = skipForwardSeconds,
                        onSelect = onSetSkipForwardSeconds
                    )
                }
            }
        }

        SettingsSectionTitle("Yedekleme (OPML)")
        SettingsCard {
            Column {
                SettingsActionRow(
                    title = "Abonelikleri OPML olarak dışa aktar",
                    description = if (subscriptionsCount > 0) {
                        "$subscriptionsCount aboneliği standart OPML 2.0 XML olarak kopyala."
                    } else {
                        "Dışa aktarılacak abonelik yok."
                    },
                    enabled = subscriptionsCount > 0,
                    onClick = {
                        opmlCopied = false
                        exportedOpml = onExportOpml()
                    }
                )
                SettingsActionRow(
                    title = "OPML içe aktar",
                    description = if (isImportingOpml) {
                        "OPML beslemeleri içe aktarılıyor..."
                    } else {
                        "Başka bir uygulamadan aldığın OPML XML içeriğini yapıştırarak toplu abone ol."
                    },
                    enabled = !isImportingOpml,
                    onClick = {
                        importInput = ""
                        showImportDialog = true
                    }
                )
            }
        }

        SettingsSectionTitle("Bildirimler")
        SettingsCard {
            Column {
                if (isBackgroundRefreshSupported) {
                    SettingsSwitchRow(
                        title = "Arka planda yenile",
                        description = "Uygulama kapalıyken yaklaşık 6 saatte bir kontrol eder ve yeni bölüm olursa bildirir.",
                        checked = backgroundRefreshEnabled,
                        onCheckedChange = onSetBackgroundRefresh
                    )
                }
                SettingsActionRow(
                    title = "Sistem bildirim izinleri",
                    description = if (systemNotificationsEnabled) {
                        "Bildirimler açık — kanal ve izin tercihlerini yönet."
                    } else {
                        "Bildirimler kapalı — sistem ayarlarından aç."
                    },
                    enabled = true,
                    onClick = onOpenNotificationSettings
                )
            }
        }

        SettingsSectionTitle("Depolama ve Veri")
        SettingsCard {
            Column {
                SettingsActionRow(
                    title = "Tüm indirmeleri sil",
                    description = if (downloadedCount > 0) {
                        "$downloadedCount indirilmiş bölüm cihazdan silinir."
                    } else {
                        "İndirilmiş bölüm yok."
                    },
                    enabled = downloadedCount > 0,
                    onClick = { confirmDeleteDownloads = true }
                )
                SettingsActionRow(
                    title = "Dinleme geçmişini ve istatistikleri temizle",
                    description = "Dinlendi işaretlerini, kalınan yerleri, dinleme süresini ve “En Son Dinlediklerim” listesini siler.",
                    enabled = hasListeningHistory,
                    onClick = { confirmClearHistory = true }
                )
            }
        }

        SettingsSectionTitle("Gizlilik ve Hakkında")
        SettingsCard {
            Column {
                SettingsActionRow(
                    title = "Gizlilik Politikası",
                    description = "Yerel veri saklama, ağ istekleri ve izin kullanımları hakkında bilgi al.",
                    enabled = true,
                    onClick = { showPrivacyDialog = true }
                )
                SettingsActionRow(
                    title = "Açık Kaynak Lisansları",
                    description = "Compose Multiplatform, Media3, Ktor, Coil ve kullanılan diğer kütüphaneler.",
                    enabled = true,
                    onClick = { showLicensesDialog = true }
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Uygulama Bilgisi",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Waveform v$versionName • com.cnsoftware.waveform",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    val opmlText = exportedOpml
    if (opmlText != null) {
        AlertDialog(
            onDismissRequest = { exportedOpml = null },
            title = { Text("OPML Dışa Aktarma") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = opmlText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 240.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                    if (opmlCopied) {
                        Text(
                            text = "Panoya kopyalandı.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(opmlText))
                    opmlCopied = true
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kopyala")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedOpml = null }) { Text("Kapat") }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("OPML İçe Aktar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "<outline xmlUrl=\"...\"/> içeren OPML XML metnini yapıştır:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        placeholder = { Text("<opml version=\"2.0\">...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 130.dp, max = 220.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportDialog = false
                        onImportOpml(importInput)
                    },
                    enabled = importInput.isNotBlank()
                ) { Text("İçe Aktar") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("İptal") }
            }
        )
    }

    if (confirmDeleteDownloads) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDownloads = false },
            title = { Text("Tüm indirmeler silinsin mi?") },
            text = { Text("İndirilmiş $downloadedCount bölüm cihazdan silinir. Bölümleri sonra yeniden indirebilirsin.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteDownloads = false
                    onDeleteAllDownloads()
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDownloads = false }) { Text("İptal") }
            }
        )
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Dinleme geçmişi temizlensin mi?") },
            text = { Text("Bu işlem geri alınamaz. Abonelikler, yer imleri ve indirilen bölümler silinmez.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearHistory = false
                    onClearListeningHistory()
                }) { Text("Temizle") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) { Text("İptal") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Gizlilik Politikası") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Waveform hesap gerektirmez, reklam göstermez ve analitik/izleme SDK'sı içermez.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Yerel Depolama: Aboneliklerin, dinleme geçmişin, istatistiklerin, favorilerin, yer imlerin, arama geçmişin ve çevrimdışı indirdiğin ses dosyaları yalnızca cihazında tutulur. İndirilen ses dosyaları bulut yedeklemelerine dahil edilmez.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Arama İstekleri: Keşfet sekmesinde arama yaptığında arama sorgun yalnızca sonuçları getirmek amacıyla Apple iTunes Search API servisine gönderilir.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Podcast Beslemeleri ve Medya: RSS beslemeleri, kapak görselleri ve ses dosyaları doğrudan yayıncının sunucusundan çekilir.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Veri Silme: Ayarlar içindeki temizleme seçenekleriyle tüm yerel verilerini veya indirmelerini dilediğin zaman silebilirsin.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Kapat") }
            }
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Açık Kaynak Lisansları") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Waveform aşağıdaki açık kaynak kütüphaneleri kullanır (Apache License 2.0):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        "JetBrains Compose Multiplatform" to "UI, Material 3 ve Çoklu Platform Kaynakları",
                        "AndroidX Media3 (ExoPlayer & MediaSession)" to "Arka plan ses oynatma ve medya bildirimleri",
                        "AndroidX WorkManager & Core KTX" to "Periyodik arka plan yenileme görevleri",
                        "Ktor Client (OkHttp & Darwin)" to "RSS besleme ve medya ağ istekleri",
                        "Coil 3" to "Podcast ve bölüm kapak görseli önbellekleme",
                        "Kotlinx Coroutines & Serialization" to "Eşzamanlılık ve JSON veri saklama"
                    ).forEach { (name, desc) ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) { Text("Kapat") }
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        content()
    }
}
