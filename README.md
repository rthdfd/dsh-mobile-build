# dsh-mobile-build

用 GitHub Actions 编译上游 [Thanksgiver233/dsh-mobile](https://github.com/Thanksgiver233/dsh-mobile) 的 Android APK。

## 为什么需要这个仓库

上游仓库**没有任何 GitHub Actions 工作流**（4 个 commit 全是源码提交），而且源码本身**编译不过**：
缺 Gradle wrapper jar、`gradle.properties` 是残缺配置、多个 Kotlin 文件引用了不存在的类型/API。

本仓库不存源码，只存"构建修复层"：

- `.github/workflows/build.yml` —— 每次运行都拉上游最新源码 → 应用 `overlay/` 修复 → 编译 debug APK
- `overlay/` —— 修复后的文件，按原路径平铺，构建时直接覆盖上游同名文件

## 怎么跑

推送任何 commit，或在 Actions 页面手动 `Run workflow`。

产物：
- `dsh-mobile-debug-apk` —— 编译出的 APK
- `build-log` —— 完整 Gradle 日志（失败时用来定位问题）

## 修好了哪些上游问题

| 类别 | 问题 |
| --- | --- |
| 构建配置 | `gradle.properties` 缺 `android.useAndroidX=true` 等必需项 |
| 构建配置 | 仓库未提交 `gradle-wrapper.jar`；覆盖 `gradlew` 为自举脚本（直接下载官方 Gradle 发行包） |
| 依赖 | `libs.versions.toml` 缺 `material-icons-extended`、`room-compiler` 别名 |
| 依赖 | `core-ktx 1.15.0` 要求 compileSdk 35，锁到 `1.13.1` |
| 依赖 | 补 `hilt-navigation-compose` |
| Room | `@RoomDatabase` / `@Entity` 注解不存在，`SessionEntity.Message` 没有实体定义，参数名不匹配 |
| Dagger | `AppComponent` 与 Hilt 的 `@ApplicationContext` 绑定冲突（且无人引用）→ 移除 |
| Dagger | `DatabaseModule` 缺 `import androidx.room.Room` |
| 源码 | `DSHApplication` 引用不存在的 `AppContainer` |
| 源码 | `DshEngineManager` 引用不存在的 `ApplicationProvider`、`withContext(Executor)` 用法错误、`Process.pid()` 在 Android 上不可用、字符串模板笔误 |
| 源码 | `NetworkHelper` 使用 OkHttp 4 已删除的 `MediaType.parse` / `RequestBody.create` |
| 源码 | `ui/legacy/LegacyWebViewActivity.kt` 里重复定义了 `MainActivity` |
| 源码 | `NavigationHost` 缺 `nav/compose` 导入、缺 `navArgument`、`viewModel()` 应为 `hiltViewModel()` |
| 源码 | `SettingsScreen` 调用不存在的 `modelSelector`；`ChatScreen` 给 FAB 传了不支持的 `enabled` 参数、越界滚动 |
| 资源 | `colors.xml` 只有 1 个颜色，但主题/布局引用了 6 个颜色 |
| 资源 | `activity_main.xml` 缺少代码里用到的 `webView` / `progressBar` / `emptyStateView` / `errorTextView` |
| 清单 | 启动 Activity 指向 `.ui.MainActivity`（应为 `.MainActivity`），且从未启动引擎服务 |

## 已知限制

- 上游**没有打包引擎二进制**（没有 `app/src/main/assets`）。`BUILD.md` 要求自行放入
  `assets/engine/node`（ARM64 Node.js）与 `assets/engine/dsh`（dsh CLI）。
  因此当前编出的 APK 只有 UI 壳，本地引擎无法启动。
- debug 签名，包名带 `.debug` 后缀，仅供测试安装。
