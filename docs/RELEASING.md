# Android APK 发布与验证

正式发布使用 Android `release` 变体。仓库不包含签名密钥、密码或已签名 APK；GitHub Actions 仅运行构建与测试，不自动发布，也不会打包未适配的桌面端。

1. 使用 JDK 21 和 Android SDK 34 运行 `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assembleRelease`，确认全部通过。
2. 用项目专用、妥善备份的私有签名密钥对 `app/build/outputs/apk/release/app-release-unsigned.apk` 先 `zipalign`、后 `apksigner sign`。不要在命令行历史、仓库、CI 日志或 Release 中暴露密码或密钥。发布密钥遗失后将无法以相同包名正常升级已有安装。
3. 用 `apksigner verify --verbose --print-certs` 检查 v2/v3 签名、证书摘要与非调试包；再检查版本号、包名、权限、内置许可文件、SHA-256 和安装/启动结果。更换密钥会阻断现有安装的直接升级。
4. GitHub Release 只上传检查过的已签名 Android APK，并在发行说明中写明 Android 17 验证范围、Shizuku 前提、还原方式及其他机型未验证的限制。

本项目未承诺 Android 17 所有厂商机型均可使用；可复现的失败请附设备型号、系统版本、两张 SIM 的状态及脱敏日志，不要公开 IMSI、手机号或其他个人数据。
