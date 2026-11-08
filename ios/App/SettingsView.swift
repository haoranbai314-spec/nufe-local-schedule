import SwiftUI
import WebKit

struct SettingsView: View {
    @EnvironmentObject var store: ScheduleStore
    @Environment(\.dismiss) var dismiss
    @State private var delete = false
    @State private var message: String?
    var body: some View {
        NavigationStack { Form {
            #if FREE_SIGNING
            Section("体验版") { Text("此版本提供本地课表与教务导入，未包含桌面小组件。") }
            #else
            Section("桌面小组件") { Text("长按手机桌面空白处，点编辑 → 添加小组件，搜索南财课表。可选今天课程或下一节课。"); Text("小组件不能自由上下滚动，点击可打开完整课表；系统可能延迟刷新。").foregroundStyle(.secondary) }
            #endif
            Section("本地数据") {
                Button("清除学校登录状态") { let types: Set<String> = [WKWebsiteDataTypeCookies, WKWebsiteDataTypeLocalStorage, WKWebsiteDataTypeSessionStorage]; WKWebsiteDataStore.default().removeData(ofTypes: types, modifiedSince: .distantPast) { message = "登录状态已清除，课表仍保留。" } }
                Button("删除本地课表", role: .destructive) { delete = true }
            }
            Section("隐私与版本") {
                Text("iOS 预览版 " + (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""))
                #if FREE_SIGNING
                Text("课表保存在本 App 的手机本地空间。")
                #else
                Text("课表保存在手机和应用共享容器。")
                #endif
                Text("教务登录由学校页面处理，导入后可离线查看。教务调课需手动重新导入。无广告、无统计。")
                Text("非官方工具 · GPL-3.0-or-later")
            }
        }.navigationTitle("设置").toolbar { Button("完成") { dismiss() } }
            .confirmationDialog("删除课表后需要重新导入", isPresented: $delete, titleVisibility: .visible) { Button("删除", role: .destructive) { store.clear() } }
            .alert("提示", isPresented: Binding(get: { message != nil }, set: { if !$0 { message = nil } })) { Button("知道了") { message = nil } } message: { Text(message ?? "") }
        }
    }
}
