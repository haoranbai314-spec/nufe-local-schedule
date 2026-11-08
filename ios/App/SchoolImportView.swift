import SwiftUI
import WebKit

@MainActor final class SchoolBrowser: NSObject, ObservableObject, WKNavigationDelegate {
    static let page = URL(string: "https://jwxt.nufe.edu.cn/student/for-std/course-table")!
    let web: WKWebView
    @Published var status = "本人登录学校教务，进入课表后点击读取。"
    @Published var reading = false
    @Published var raw: Data?
    private var generation = 0
    override init() { web = WKWebView(frame: .zero, configuration: WKWebViewConfiguration()); super.init(); web.navigationDelegate = self; web.load(URLRequest(url: Self.page)) }
    static func allowed(_ url: URL?) -> Bool { guard let url, url.scheme == "https", let host = url.host?.lowercased() else { return false }; return host == "nufe.edu.cn" || host.hasSuffix(".nufe.edu.cn") }
    func navigation() { generation += 1; reading = false; web.load(URLRequest(url: Self.page)) }
    func cancel() { generation += 1; web.stopLoading(); reading = false }
    func webView(_ webView: WKWebView, decidePolicyFor action: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) { guard Self.allowed(action.request.url) else { status = "仅允许访问学校 HTTPS 页面。"; decisionHandler(.cancel); return }; if action.targetFrame == nil { webView.load(action.request); decisionHandler(.cancel) } else { decisionHandler(.allow) } }
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) { status = "连接失败：" + error.localizedDescription }
    func capture() {
        guard !reading, let url = web.url, url.host == "jwxt.nufe.edu.cn", url.path.hasPrefix("/student/for-std/course-table") else { status = "请先登录并进入学校课表页面。"; return }
        reading = true; generation += 1; let token = generation; status = "正在读取课表…"
        Task { @MainActor in
            defer { if token == generation { reading = false } }
            do {
                guard let scriptURL = Bundle.main.url(forResource: "school-import", withExtension: "js") else { throw ImportError.invalid("缺少导入脚本") }
                _ = try await web.evaluateJavaScript(String(contentsOf: scriptURL, encoding: .utf8))
                for _ in 0..<90 {
                    try await Task.sleep(nanoseconds: 500_000_000)
                    guard token == generation else { return }
                    guard let current = web.url, current.host == "jwxt.nufe.edu.cn", current.path.hasPrefix("/student/for-std/course-table") else { throw ImportError.invalid("页面已离开课表，请重试") }
                    let value = try await web.evaluateJavaScript("JSON.stringify(window.__nufeLocalImport || {state:'waiting'})")
                    guard let text = value as? String, let bytes = text.data(using: .utf8), let result = try JSONSerialization.jsonObject(with: bytes) as? [String: Any] else { continue }
                    if result["state"] as? String == "error" { throw ImportError.invalid(result["error"] as? String ?? "读取失败") }
                    if result["state"] as? String == "done", let schedule = result["schedule"] as? [String: Any] { raw = try JSONSerialization.data(withJSONObject: schedule); status = "读取完成，请核对开学日期。"; return }
                }
                throw ImportError.invalid("读取超时，请重试")
            } catch { if token == generation { status = error.localizedDescription } }
        }
    }
}

struct SchoolWebView: UIViewRepresentable {
    let browser: SchoolBrowser
    func makeUIView(context: Context) -> WKWebView { browser.web }
    func updateUIView(_ uiView: WKWebView, context: Context) {}
}

struct SchoolImportView: View {
    @EnvironmentObject var store: ScheduleStore
    @Environment(\.dismiss) var dismiss
    @StateObject private var browser = SchoolBrowser()
    @State private var weekOne = SchoolClock.monday(Date())
    @State private var dateProvided = false
    @State private var confirm = false
    @State private var error: String?
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Text(browser.status).font(.caption).foregroundStyle(.secondary).padding(12).frame(maxWidth: .infinity, alignment: .leading)
                SchoolWebView(browser: browser)
            }.navigationTitle("学校教务").navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("返回") { browser.cancel(); dismiss() } }
                    ToolbarItemGroup(placement: .primaryAction) { Button("课表") { browser.navigation() }; Button("读取") { browser.capture() }.disabled(browser.reading) }
                }
                .onChange(of: browser.raw) { _, raw in
                    guard let raw else { return }
                    if let root = try? JSONSerialization.jsonObject(with: raw) as? [String: Any], let value = root["week1_monday"] as? String, let date = SchoolClock.parseDay(value) { weekOne = date; dateProvided = true } else { dateProvided = false }
                    confirm = true
                }
                .sheet(isPresented: $confirm) {
                    NavigationStack { Form {
                        Text(dateProvided ? "请核对第一周的周一日期。日期错误会让整学期错周。" : "学校没有提供开学日期。请按校历设置第一周的周一，当前日期只是占位。")
                        DatePicker("第一周周一", selection: $weekOne, displayedComponents: .date)
                        if let error { Text(error).foregroundStyle(.red) }
                        Button("保存到手机") {
                            do { guard let raw = browser.raw else { return }; let schedule = try Schedule.imported(raw, weekOne: weekOne); try store.save(schedule); confirm = false; dismiss() } catch { self.error = error.localizedDescription }
                        }
                    }.navigationTitle("确认课表").toolbar { Button("取消") { confirm = false } } }.environment(\.calendar, SchoolClock.calendar).environment(\.timeZone, SchoolClock.calendar.timeZone)
                }.onDisappear { browser.cancel() }
        }
    }
}
