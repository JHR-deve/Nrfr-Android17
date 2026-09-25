package com.github.nrfr.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.nrfr.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
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
                        Text("关于")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .weight(1f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 应用信息
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "功能介绍",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "• 仅覆盖系统报告的 SIM 国家码，不修改 SIM 本体或运营商名称\n" +
                                    "• 使用 Shizuku 和短时 instrumentation，写入非持久配置\n" +
                                    "• 写前保存原国家码，恢复时核对 SIM 运营商数字码\n" +
                                    "• 重启、SIM 刷新或系统更新可能使覆盖失效\n" +
                                    "• 已在 K90 验证的行为不保证在其他机型上可用",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 作者信息
                Text(
                    "作者信息",
                    style = MaterialTheme.typography.titleMedium
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("上游作者: Ackites；本地改版: JHR-deve")
                        Text(
                            "GitHub: Ackites",
                            modifier = Modifier.clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ackites"))
                                )
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "X (Twitter): @actkites",
                            modifier = Modifier.clickable {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://x.com/intent/follow?screen_name=actkites")
                                    )
                                )
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 开源信息
                Text(
                    "开源信息",
                    style = MaterialTheme.typography.titleMedium
                )
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            // Local derivative: this copy has not been published.
                            "本地改版，基于 Ackites/Nrfr（Apache-2.0）",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "查看上游项目",
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/Ackites/Nrfr")
                                        )
                                    )
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 版权信息
                Text(
                    "Nrfr: Apache-2.0；兼容层: MIT。详见仓库 NOTICE。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
