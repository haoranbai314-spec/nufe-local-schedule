# 南财课表 iOS 原生版

SwiftUI + WKWebView + WidgetKit，最低 iOS 17。与安卓工程独立，安卓安装包不受影响。

## 当前功能

- 学校 HTTPS 教务页面登录，读取真实课表，核对开学第一周周一后保存。
- 按上课周次显示七天周课表与逐日课程明细，点击课程查看完整信息。
- 本地离线保存，App Groups 与两个桌面小组件共享课表。
- 今天课程小组件、下一节课与倒计时小组件；点击打开应用。
- 清除登录状态与删除课表。没有统计、广告或远端课表账户。

iOS 组件不提供安卓式上下滚动；系统决定刷新时间，课程边界不保证准点刷新。首次开机解锁前共享文件不可读。当前没有实现 Live Activity、锁屏课程通知、系统日历导入与 App Store/TestFlight 发布。

## 编译

在 Mac（或 GitHub Actions 的 macOS runner）安装 Xcode 和 XcodeGen：

```sh
brew install xcodegen
cd ios
xcodegen generate
xcodebuild -project NufeSchedule.xcodeproj -scheme NufeSchedule -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath build CODE_SIGNING_ALLOWED=NO build
```

生成的模拟器 `.app` 不能安装到真实 iPhone。Windows 不能运行 Xcode 或 iOS 模拟器。

真机安装仍需签名：在苹果开发者账号中注册自己的 App、Widget Bundle ID 和 App Group，将 `project.yml` 中两个 Bundle ID、`APP_GROUP_ID` 改成自己的标识。Xcode 中为 App 与 Widget 同时选择同一 Team、同一 App Group，然后编译签名。TestFlight / App Store 还需要付费开发者计划及发布配置。

`school-import.js` 复用安卓项目解析，实际学校登录与导入仍需真机验证。没有把安卓学校登录凭据或用户课表复制进本工程。签名、证书、学校密码不应提交仓库。

## 云端构建

仓库根目录 `.github/workflows/ios-build.yml` 可手动触发原生模拟器构建。它不使用苹果证书，不发布 TestFlight，不产出可直接安装的 IPA。构建结果下载至 GitHub Actions 的 Artifacts。

GPL-3.0-or-later；教务解析参考 LokmenoWer/nufe-kebiao，与学校无隶属关系。

## 编译记录
已完成首轮 macOS 云编译：App 与两个 WidgetKit 小组件构建成功（iOS Simulator 18.5 SDK）。后续云构建使用 macOS 26 runner 的默认新版 Xcode。未完成真机登录导入及签名安装验证。云端同时提供已生成的 Xcode 工程源码压缩包。

## 免费签名体验版（真机 IPA）

`project-free.yml` 生成独立的 App-only 工程，版本 0.1.1、包名 cn.local.nufeschedule.ios.free。以 FREE_SIGNING 编译条件使用 App 私有 Application Support 保存课表，不依赖 App Groups，也不包含 Widget 扩展。

仓库工作流 `iOS device IPA for personal sideloading` 用 iphoneos SDK 和 arm64 生成真机程序，检查平台为 iPhoneOS 后封装 Payload/*.app 为未签名 IPA。该文件需要在 Windows 的 Sideloadly / AltStore 中用自己的 Apple 账号重新签名安装；它不能直接点开安装。免费账号的签名通常需要每7天刷新，安装以手机系统与签名工具的结果为准。

安装时保持同一 Apple 账号和包名可用于覆盖升级。不要先删除 App，否则本地课表会被清除。首次可能需要在 iPhone 的设置 → 隐私与安全性开启开发者模式，以及设置 → 通用 → VPN与设备管理中信任开发者。

此流程不向云构建提供 Apple 密码、证书或学校账号。真实教务登录与数据导入仍需 iPhone 实测。
