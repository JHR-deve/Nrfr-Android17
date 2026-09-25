# 来源与许可声明

本项目基于 [Ackites/Nrfr](https://github.com/Ackites/Nrfr) 的 `c8044fe7d42beea496d66fae43b9683fcf942ce4` 版本，沿用 Apache License 2.0；原版 [LICENSE](LICENSE) 保持不变。本项目不是原作者的官方发布。

Android 17 适配修改了运营商配置写入路径、还原保护、应用包名和界面。兼容桥、权限握手及 instrumentation 的部分实现改编自 [Ritel-T/SamsungRegionOverride](https://github.com/Ritel-T/SamsungRegionOverride)，遵循 MIT 许可，完整文本见 [licenses/SamsungRegionOverride-MIT.txt](licenses/SamsungRegionOverride-MIT.txt)。相关源文件标有来源。

Android APK 的 assets 内附有 Apache-2.0 和上述 MIT 许可全文，便于独立下载 APK 的用户查看。

仓库保留上游 `nrfr-client` 桌面端源码，但该组件尚未适配本版应用包名，不包含在本版 Android APK 的已验证功能中。设备和厂商兼容性限制见 [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)。
