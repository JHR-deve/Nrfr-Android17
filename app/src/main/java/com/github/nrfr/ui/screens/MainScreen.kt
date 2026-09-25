package com.github.nrfr.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.nrfr.R
import com.github.nrfr.compat.CountryOverrideCoordinator
import com.github.nrfr.data.CountryPresets
import com.github.nrfr.manager.CarrierConfigManager
import com.github.nrfr.model.SimCardInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onShowAbout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSubId by remember { mutableIntStateOf(-1) }
    var selectedCountryCode by remember { mutableStateOf("") }
    var customCountryCode by remember { mutableStateOf(false) }
    var showCountrySheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf("") }

    val simCards = remember(context, refreshTrigger) { CarrierConfigManager.getSimCards(context) }
    val selectedSim = simCards.find { it.subId == selectedSubId }
        ?: simCards.find { it.isDefaultData }
        ?: simCards.firstOrNull()
    val restoreTarget = selectedSim?.let {
        CountryOverrideCoordinator.restoreTarget(context, it.subId, it.operatorNumeric)
    }
    val restoreAvailable = selectedSim?.let {
        restoreTarget != null && (CountryOverrideCoordinator.hasSnapshot(context, it.subId)
            || !it.countryIso.equals(restoreTarget, ignoreCase = true))
    } == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_app_mark),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Nrfr")
                    }
                },
                actions = {
                    IconButton(onClick = onShowAbout) {
                        Icon(Icons.Default.Info, contentDescription = "关于")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("选择 SIM", style = MaterialTheme.typography.titleMedium)
            if (simCards.isEmpty()) {
                Text("未读取到已激活的 SIM。请检查电话权限、SIM 状态或设备兼容性。")
            }
            simCards.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { sim ->
                        SimCardTile(
                            sim = sim,
                            selected = sim.subId == selectedSim?.subId,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedSubId = sim.subId
                                selectedCountryCode = ""
                                customCountryCode = false
                                lastResult = ""
                            }
                        )
                    }
                    if (pair.size == 1 && simCards.size > 1) Spacer(Modifier.weight(1f))
                }
            }

            Text("选择伪装地区", style = MaterialTheme.typography.titleMedium)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = selectedSim != null && !isBusy) { showCountrySheet = true },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        flagFor(selectedCountryCode),
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        val preset = CountryPresets.countries.find {
                            it.code == selectedCountryCode
                        }
                        Text(
                            preset?.name ?: if (selectedCountryCode.isEmpty())
                                "点击选择国家或地区" else "自定义地区",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selectedCountryCode.isNotEmpty()) {
                            Text(selectedCountryCode, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("选择", color = MaterialTheme.colorScheme.primary)
                }
            }
            if (customCountryCode) {
                val focusManager = LocalFocusManager.current
                OutlinedTextField(
                    value = selectedCountryCode,
                    onValueChange = { value ->
                        if (value.length <= 2 && value.all { it in 'A'..'Z' || it in 'a'..'z' }) {
                            selectedCountryCode = value.uppercase(Locale.ROOT)
                        }
                    },
                    label = { Text("自定义两位国家码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                "只覆盖系统报告的 SIM 国家码，不修改 SIM 本体或运营商名称。" +
                    "网络地区可能仍显示实际接入地区。已识别的中国运营商卡按运营商编码还原；" +
                    "其他卡使用本应用保存的原值。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (lastResult.isNotEmpty()) {
                Text(lastResult, style = MaterialTheme.typography.bodyMedium)
            }
            if (isBusy) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val sim = selectedSim ?: return@OutlinedButton
                        isBusy = true
                        scope.launch {
                            val result = runCatching {
                                withContext(Dispatchers.IO) {
                                    CarrierConfigManager.restoreCountry(context, sim.subId)
                                }
                            }
                            lastResult = result.fold(
                                { "设置已还原" },
                                { "恢复失败: ${it.message}" }
                            )
                            Toast.makeText(context, lastResult, Toast.LENGTH_LONG).show()
                            refreshTrigger++
                            isBusy = false
                        }
                    },
                    enabled = restoreAvailable && !isBusy,
                    modifier = Modifier.weight(1f)
                ) { Text("还原设置") }
                Button(
                    onClick = {
                        val sim = selectedSim ?: return@Button
                        val target = selectedCountryCode
                        isBusy = true
                        scope.launch {
                            val result = runCatching {
                                withContext(Dispatchers.IO) {
                                    CarrierConfigManager.setCountry(context, sim.subId, target)
                                }
                            }
                            lastResult = result.fold(
                                { "国家码已生效" },
                                { "保存失败: ${it.message}" }
                            )
                            Toast.makeText(context, lastResult, Toast.LENGTH_LONG).show()
                            refreshTrigger++
                            isBusy = false
                        }
                    },
                    enabled = selectedSim != null && !isBusy &&
                        selectedCountryCode.matches(Regex("[A-Z]{2}")),
                    modifier = Modifier.weight(1f)
                ) { Text("保存生效") }
            }
        }
    }

    if (showCountrySheet) {
        ModalBottomSheet(onDismissRequest = { showCountrySheet = false }) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("选择伪装地区", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("搜索国家、地区或代码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val countries = CountryPresets.countries.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
                }
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    item {
                        CountryRow("🌐", "自定义两位国家码", "", onClick = {
                            selectedCountryCode = ""
                            customCountryCode = true
                            showCountrySheet = false
                            searchQuery = ""
                        })
                    }
                    items(countries, key = { it.code }) { country ->
                        CountryRow(
                            flagFor(country.code), country.name, country.code,
                            onClick = {
                                selectedCountryCode = country.code
                                customCountryCode = false
                                showCountrySheet = false
                                searchQuery = ""
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimCardTile(
    sim: SimCardInfo,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val country = sim.countryIso.takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "--"
    val network = sim.networkCountryIso.takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "--"
    Card(
        modifier = modifier
            .heightIn(min = 220.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SIM ${sim.slot}", style = MaterialTheme.typography.bodyMedium)
                if (sim.isDefaultData) {
                    Spacer(Modifier.width(5.dp))
                    Surface(shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("数据", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (selected) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text("✓", modifier = Modifier.padding(horizontal = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    Surface(shape = CircleShape, border = BorderStroke(1.dp,
                        MaterialTheme.colorScheme.outline)) {
                        Spacer(Modifier.size(22.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(flagFor(country), fontSize = 27.sp)
                Spacer(Modifier.width(5.dp))
                Text(country, fontSize = 27.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                "${sim.operatorNumeric.ifEmpty { "--" }} · ${sim.carrierName.ifEmpty { "未知运营商" }}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (network != country) "网络地区仍为 $network" else "网络地区 $network",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("sub ${sim.subId}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(if (sim.isReady) "就绪" else "未就绪",
                    style = MaterialTheme.typography.bodySmall)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text("系统地区", modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CountryRow(flag: String, name: String, code: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(flag, fontSize = 27.sp)
        Spacer(Modifier.width(14.dp))
        Text(name, modifier = Modifier.weight(1f))
        if (code.isNotEmpty()) Text(code, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun flagFor(countryIso: String): String {
    val code = countryIso.uppercase(Locale.ROOT)
    if (!code.matches(Regex("[A-Z]{2}"))) return "🌐"
    val first = String(Character.toChars(0x1F1E6 + code[0].code - 'A'.code))
    val second = String(Character.toChars(0x1F1E6 + code[1].code - 'A'.code))
    return first + second
}
