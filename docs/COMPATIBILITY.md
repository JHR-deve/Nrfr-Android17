# 兼容性与测试范围

本项目是 Ackites/Nrfr 的非官方 Android 17 适配版。原版调用 `CarrierConfigManager.overrideConfig()` 时在部分 Android 17 系统上会因 shell 权限限制而保存失败。本版通过 Shizuku 启动短时 instrumentation，临时委托电话状态权限，并只写入非持久的 `sim_country_iso_override_string`。它不会修改实体 SIM、MCC/MNC、IMSI、运营商名称或其他 CarrierConfig 项。

## 安全边界

- 操作前记录国家码与运营商数字编码；运营商编码变化时拒绝套用旧快照或还原。
- 对已识别的中国大陆运营商卡，以运营商编码确定还原目标 `CN`；其他卡使用本应用保存的原值。无法可靠确定时不写入。
- “还原设置”只写回该国家码，不以空配置清除同一订阅上的其他工具配置。
- 配置是非持久覆盖，重启、SIM 刷新或系统更新后可能失效。关闭本应用不等于立即还原。
- 双卡需逐张选择并设置。网络地区仍可能显示实际接入地区；本功能主要用于应用读取 SIM 所属国家/地区的场景。

## 已验证与未验证

Android 17 / API 37 实机已验证双 SIM 国家码修改；此前调试包完成过单卡 JP→SG→JP 循环，强制停止应用后覆盖仍保留，再次打开可还原。正式签名 APK 已在 Android 17 安装、启动，双卡读取、关于页与返回逻辑检查通过；此次发布检查没有再次写入 SIM 配置。不同系统厂商的实现可能有差异。

正式版包名为 `io.github.jhrdeve.nrfr`。此前本地测试包 `io.github.jhrdeve.nrfrk90` 可并存，但两者的还原快照不共享；若曾用旧包修改国家码，先在旧包内还原，再切换正式版，避免以已伪装值作为新快照。上游桌面 `nrfr-client` 仍针对原版包名，不能用于此版。

静态分析注意：原有 AGP 8.7.0-rc01 / Kotlin 2.0.0 组合下，`lintDebug` 在 Compose 检查器 `AutoboxingStateCreation` 和 `MutableCollectionMutableState` 内崩溃。不能将其视为通过 lint。发布前需要分别运行单元测试、构建、APK 签名检查和设备验证。
