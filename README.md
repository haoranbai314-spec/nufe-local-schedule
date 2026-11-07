# 南财课表 · 本地

非官方安卓课表工具：在本机登录学校教务并导入课表、离线查看、独立桌面课表组件，以及应用内检查更新。支持安卓 8.0 及以上；安卓 12 及以上使用分时段组件网格。

## 安装与更新

从本仓库的 [Releases](https://github.com/haoranbai314-spec/nufe-local-schedule/releases/latest) 下载 `nufe-local.apk`，按系统提示安装。已有版本可覆盖安装，保留本地课表。

打开软件 → 设置 → 检查软件更新，可查看新版说明并下载安装。首次需允许本 App 安装应用，最终安装由系统确认。GitHub 在部分网络可能无法连接。

首次使用：登录教务 → 打开课表 → 读取课表 → 核对第一周周一日期并保存。在应用中添加桌面组件，或长按桌面空白处，在组件列表选择“南财课表·本地”。

## 数据与权限

课表保存在本机，不依赖系统日历。学校登录通过本机 WebView 进行；检查软件更新只访问 GitHub 发布接口，不上传课表。课表需要手动更新，支持可选的锁屏下一节课卡片，默认无声音和振动，系统省电可能延迟课程切换。

## 修改与构建

源码位于 `src`、`res`、`assets`。Windows 下运行 `build.ps1`，通过 `-Jdk`、`-Sdk` 指定 JDK 17 和 Android SDK（android-35、build-tools 35.0.0），通过 `-Output` 指定输出 APK。

首次构建自动创建本地开发签名。自行编译的签名通常与本仓库发行版不同，不能直接覆盖发行版。请备份并保护签名文件，勿公开上传。

发布须递增 `AndroidManifest.xml` 的 `versionCode`、更新 `versionName`，使用同一私有签名构建。`Publish-Update.ps1` 构建并发布 APK 和不含私有签名文件的源码包，具体见 `UPDATES.txt`。

## 许可证

GPL-3.0-or-later。学校教务字段解析参考 [LokmenoWer/nufe-kebiao](https://github.com/LokmenoWer/nufe-kebiao)。本项目与学校及原项目作者无隶属关系。

