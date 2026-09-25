# Nrfr · Android 17 适配版

Nrfr 是一款**免 Root 的 SIM 国家码修改工具**：通过 Shizuku 调整 Android 系统向应用报告的 SIM 所属国家/地区，适用于应用地区识别、地区相关功能适配、漫游及海外 SIM 本地化等场景。本项目基于 [Ackites/Nrfr](https://github.com/Ackites/Nrfr) 改版，修复原版在 Android 17 上保存配置时报错的问题。

## 主要功能

- 免 Root 修改系统报告的 SIM 国家码，无需修改实体 SIM 或系统文件。
- 支持双卡分别设置，并可在应用内还原设置。
- 提供带国旗的常用地区选择和自定义两位国家码；双卡状态卡片便于查看当前配置。
- 保留原版 Android 端的主要功能，修复 Android 17 保存失败，并优化操作界面。

本应用只覆盖系统报告的 SIM 国家码，不修改实体 SIM、MCC/MNC、IMSI 或运营商名称。配置为非持久覆盖，重启、SIM 刷新或系统更新后可能失效。双卡修改已在 Android 17 实机验证；不同厂商的实现可能有差异。

## 使用

1. 安装并启动 [Shizuku](https://github.com/RikkaApps/Shizuku)，授权 Nrfr，并允许读取手机状态。
2. 选择需要修改的 SIM 卡，再从带国旗的列表选择目标地区；双卡需分别操作。
3. 需要撤销本应用的设置时，使用“还原设置”。还原仅处理国家码，不清除其他工具写入的整个 CarrierConfig。

正式版 APK 在本仓库的 [Releases](https://github.com/JHR-deve/Nrfr-Android17/releases) 下载。应用包名为 `io.github.jhrdeve.nrfr`，可与原版及早期 `io.github.jhrdeve.nrfrk90` 测试包并存；不同包名的数据和 Shizuku 授权不会自动迁移。桌面快速启动器 `nrfr-client` 仅保留上游源码，**尚未适配此改版，不属于本版功能**。

## 开发与来源

使用 JDK 21、Android SDK 34 构建：`./gradlew :app:assembleDebug :app:testDebugUnitTest`。正式版使用独立发布密钥对非调试 APK 签名；密钥不存放在仓库。构建及验证步骤见 [发布说明](docs/RELEASING.md)，兼容性与测试范围见 [兼容性说明](docs/COMPATIBILITY.md)。

原作者：[Ackites](https://github.com/Ackites/Nrfr)；改版作者：JHR-deve。上游代码遵循 Apache-2.0，Android 17 兼容层包含来自 [SamsungRegionOverride](https://github.com/Ritel-T/SamsungRegionOverride) 的 MIT 许可代码。许可文本随 APK 一起打包，仓库详见 [LICENSE](LICENSE) 和 [NOTICE-ANDROID17.md](NOTICE-ANDROID17.md)。
