# Nrfr · Android 17 适配版

基于 [Ackites/Nrfr](https://github.com/Ackites/Nrfr) 的非官方改版。保留原版 Android 端免 Root 修改 SIM 国家码、双卡分别设置和还原等功能，并适配 Android 17。

本应用只覆盖系统报告的 SIM 国家码，不修改实体 SIM、MCC/MNC、IMSI 或运营商名称。需要 Shizuku 授权。配置为非持久覆盖，重启、SIM 刷新或系统更新后可能失效；不同厂商的实现也可能不同。已在 Android 17 实机验证，其他设备仍需单独测试。

桌面快速启动器 `nrfr-client` 保留了上游源码，但尚未适配此改版的应用包名，不属于本版已验证功能。当前提供的是调试构建，并非通用正式版。

构建 Android APK：使用 JDK 21、Android SDK 34，运行 `./gradlew :app:assembleDebug`。输出位于 `app/build/outputs/apk/debug/app-debug.apk`。

原作者：[Ackites](https://github.com/Ackites/Nrfr) · 改版作者：JHR-deve。上游代码遵循 Apache-2.0；Android 17 兼容层含来自 [SamsungRegionOverride](https://github.com/Ritel-T/SamsungRegionOverride) 的 MIT 许可代码。详见 [LICENSE](LICENSE) 和 [NOTICE-K90.md](NOTICE-K90.md)。
