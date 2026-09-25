package com.github.nrfr.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.nrfr.R
import com.github.nrfr.data.CountryPresets
import com.github.nrfr.data.PresetCarriers
import com.github.nrfr.manager.CarrierConfigManager
import com.github.nrfr.model.SimCardInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onShowAbout: () -> Unit) {
    val context = LocalContext.current
    var selectedSimCard by remember { mutableStateOf<SimCardInfo?>(null) }
    var selectedCountryCode by remember { mutableStateOf("") }
    var customCountryCode by remember { mutableStateOf("") }
    var isCustomCountryCode by remember { mutableStateOf(false) }
    var isSimCardMenuExpanded by remember { mutableStateOf(false) }
    var isCountryCodeMenuExpanded by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 获取实际的 SIM 卡信息
    val simCards = remember(context, refreshTrigger) { CarrierConfigManager.getSimCards(context) }

    // 当 simCards 更新时，更新选中的 SIM 卡信息
    LaunchedEffect(simCards, selectedSimCard) {
        if (selectedSimCard != null) {
            selectedSimCard = simCards.find { it.slot == selectedSimCard?.slot }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            modifier = Modifier.size(48.dp),
                            contentDescription = "App Icon",
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Local derivative; retain attribution in About and NOTICE.
                        Text("Nrfr K90 Lab")
                    }
                },
                actions = {
                    IconButton(onClick = onShowAbout) {
                        Icon(Icons.Default.Info, contentDescription = "关于")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (simCards.isEmpty()) {
                Text("未读取到已激活的 SIM。请检查电话权限、SIM 状态或设备兼容性。")
            }
            // SIM卡选择
            SimCardSelector(
                simCards = simCards,
                selectedSimCard = selectedSimCard,
                isExpanded = isSimCardMenuExpanded,
                onExpandedChange = { isSimCardMenuExpanded = it },
                onSimCardSelected = { selectedSimCard = it }
            )

            // 显示当前选中的 SIM 卡的配置信息
            selectedSimCard?.let { simCard ->
                CurrentConfigCard(simCard = simCard)
            }

            // 国家码选择
            CountryCodeSelector(
                selectedCountryCode = selectedCountryCode,
                isCustomCountryCode = isCustomCountryCode,
                customCountryCode = customCountryCode,
                isExpanded = isCountryCodeMenuExpanded,
                onExpandedChange = { isCountryCodeMenuExpanded = it },
                onCountryCodeSelected = { code ->
                    selectedCountryCode = code
                    isCustomCountryCode = false
                },
                onCustomSelected = {
                    isCustomCountryCode = true
                    selectedCountryCode = customCountryCode
                }
            )

            // 自定义国家码输入框
            if (isCustomCountryCode) {
                CustomCountryCodeInput(
                    value = customCountryCode,
                    onValueChange = {
                        if (it.length <= 2 && it.all { char -> char.isLetter() }) {
                            customCountryCode = it.uppercase()
                            selectedCountryCode = it.uppercase()
                        }
                    }
                )
            }

            Text(
                "仅更改系统报告的 SIM 国家码；不修改 SIM 本体或运营商名称。恢复将写回本应用保存的原值。",
                style = MaterialTheme.typography.bodySmall
            )
            if (lastResult.isNotEmpty()) {
                Text(lastResult, style = MaterialTheme.typography.bodyMedium)
            }

            // 按钮行
            ActionButtons(
                selectedSimCard = selectedSimCard,
                selectedCountryCode = selectedCountryCode,
                isCustomCountryCode = isCustomCountryCode,
                customCountryCode = customCountryCode,
                isBusy = isBusy,
                onReset = {
                    isBusy = true
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                CarrierConfigManager.restoreCountry(context, it.subId)
                            }
                        }
                        lastResult = result.fold({ "国家码已恢复" },
                            { "恢复失败: ${it.message}" })
                        Toast.makeText(context, lastResult, Toast.LENGTH_LONG).show()
                        refreshTrigger += 1
                        isBusy = false
                    }
                },
                onSave = { simCard ->
                    val countryCode = if (isCustomCountryCode) customCountryCode else selectedCountryCode
                    isBusy = true
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                CarrierConfigManager.setCountry(context, simCard.subId, countryCode)
                            }
                        }
                        lastResult = result.fold({ "国家码已生效" },
                            { "保存失败: ${it.message}" })
                        Toast.makeText(context, lastResult, Toast.LENGTH_LONG).show()
                        refreshTrigger += 1
                        isBusy = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimCardSelector(
    simCards: List<SimCardInfo>,
    selectedSimCard: SimCardInfo?,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSimCardSelected: (SimCardInfo) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selectedSimCard?.let { "SIM ${it.slot} (${it.carrierName})" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("选择SIM卡") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            simCards.forEach { simCard ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("SIM ${simCard.slot} (${simCard.carrierName})")
                            if (simCard.currentConfig.isEmpty()) {
                                Text(
                                    "无法读取当前国家码",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                simCard.currentConfig.forEach { (key, value) ->
                                    Text(
                                        "$key: $value",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        onSimCardSelected(simCard)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrentConfigCard(simCard: SimCardInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "系统当前报告的国家码",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (simCard.currentConfig.isEmpty()) {
                Text(
                "无法读取当前国家码",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                simCard.currentConfig.forEach { (key, value) ->
                    Text(
                        "$key: $value",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryCodeSelector(
    selectedCountryCode: String,
    isCustomCountryCode: Boolean,
    customCountryCode: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCountryCodeSelected: (String) -> Unit,
    onCustomSelected: () -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = when {
                isCustomCountryCode -> "自定义"
                selectedCountryCode.isEmpty() -> ""
                else -> CountryPresets.countries.find { it.code == selectedCountryCode }
                    ?.let { "${it.name} (${it.code})" }
                    ?: selectedCountryCode
            },
            onValueChange = {},
            readOnly = true,
            label = { Text("选择国家码") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            // 预设国家码列表
            CountryPresets.countries.forEach { countryInfo ->
                DropdownMenuItem(
                    text = { Text("${countryInfo.name} (${countryInfo.code})") },
                    onClick = {
                        onCountryCodeSelected(countryInfo.code)
                        onExpandedChange(false)
                    }
                )
            }
            // 自定义选项
            DropdownMenuItem(
                text = { Text("自定义") },
                onClick = {
                    onCustomSelected()
                    onExpandedChange(false)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomCountryCodeInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("自定义国家码 (2位字母)") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
            }
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarrierSelector(
    selectedCarrier: PresetCarriers.CarrierPreset?,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCarrierSelected: (PresetCarriers.CarrierPreset) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selectedCarrier?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("选择运营商") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            // 分组显示运营商
            PresetCarriers.presets
                .groupBy { it.region }
                .forEach { (region, carriers) ->
                    if (region.isNotEmpty()) {
                        val regionName = CountryPresets.countries.find { it.code == region }?.name ?: region
                        Text(
                            regionName,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        carriers.forEach { carrier ->
                            DropdownMenuItem(
                                text = { Text(carrier.name) },
                                onClick = {
                                    onCarrierSelected(carrier)
                                    onExpandedChange(false)
                                }
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

            // 自定义选项
            PresetCarriers.presets
                .filter { it.region.isEmpty() }
                .forEach { carrier ->
                    DropdownMenuItem(
                        text = { Text(carrier.name) },
                        onClick = {
                            onCarrierSelected(carrier)
                            onExpandedChange(false)
                        }
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomCarrierNameInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("自定义运营商名称") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ActionButtons(
    selectedSimCard: SimCardInfo?,
    selectedCountryCode: String,
    isCustomCountryCode: Boolean,
    customCountryCode: String,
    isBusy: Boolean,
    onReset: (SimCardInfo) -> Unit,
    onSave: (SimCardInfo) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 还原按钮
        OutlinedButton(
            onClick = { selectedSimCard?.let(onReset) },
            modifier = Modifier.weight(1f),
            enabled = selectedSimCard != null && !isBusy
        ) {
            Text("还原设置")
        }

        // 保存按钮
        Button(
            onClick = { selectedSimCard?.let(onSave) },
            modifier = Modifier.weight(1f),
            enabled = selectedSimCard != null && !isBusy &&
                (if (isCustomCountryCode) customCountryCode else selectedCountryCode)
                    .matches(Regex("[A-Za-z]{2}"))
        ) {
            Text("保存生效")
        }
    }
}
