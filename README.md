# PadMask · 平板面具

只对 **微信** 和 **QQ** 生效的平板模式伪装模块（LSPosed）。
不改系统属性、不改 build.prop、不刷 KernelSU 模块，
所有伪装只发生在目标应用自己的进程里。

> 状态：源码完整，正在打通本机构建链（aapt 需 arm64 版本）。

## 架构

```
Entry (IXposedHookLoadPackage)
 ├─ packageName 命中 com.tencent.mm / com.tencent.mobileqq 才继续
 ├─ BuildSpoofer     Build.MODEL / BRAND / MANUFACTURER / DEVICE / PRODUCT
 ├─ PropSpoofer      SystemProperties.get("ro.*") 的 Java 侧读取
 └─ DisplaySpoofer   Configuration(sw dp / size class) + Display + WindowMetrics
```

- 每层独立 try/catch，单层失败不影响其它层，也不会让宿主崩溃
- 开关与数值集中在 `Profile.java`
- QQ 默认只做屏幕/配置伪装，不动机型，便于对照

## 回退

1. LSPosed → 模块 → 取消启用 PadMask（或卸载）
2. 设置 → 应用 → 微信 / QQ → 强行停止
3. 重新打开即恢复原样

无任何持久化写入，卸载即彻底还原，不需要重启。

## 构建

```bash
apt-get install -y openjdk-17-jdk-headless aapt
./build.sh
```

依赖（路径在脚本顶部可改）：

| 组件 | 说明 |
| --- | --- |
| `aapt` | 系统包，**必须用 arm64 版**；Google build-tools 是 x86_64，在手机上跑不了 |
| `android.jar` | platform-33 解压得到的 `android-13/android.jar` |
| `d8.jar` / `apksigner.jar` | 取自 build-tools 的 `lib/`，是 Java 实现，架构无关 |

产物签名后输出到 `/sdcard/Download/DSH-Go/PadMask-1.0.apk`。

## 风险

- 修改客户端行为可能触发风控，**有封号风险**，建议先用小号验证
- 微信若检测到 LSPosed 痕迹，可能拒绝平板登录
- 目前只伪装到框架层；若微信内部有独立 pad 判定，需要继续定位

## 许可

MIT
