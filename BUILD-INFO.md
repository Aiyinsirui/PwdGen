# PwdGen v1.2.1 — 交付包说明 (pwdgen_v2)

> 生成时间：2026-09-28 (Asia/Shanghai)
> **最近更新：2026-09-29 (Asia/Shanghai) — 站点存储回退纯base64、修复GitHub同步404(.git后缀残留)**
> 包名 (applicationId)：`com.pwdgen.app`
> 版本：versionCode=4 / versionName=1.2.1
> minSdk=24 / targetSdk=35 / compileSdk=35
> 对应 Git 提交：`80627f9` (master)

---

## 一、本次交付包含的改动


### J. 站点存储回退纯base64 + 修复GitHub同步404（v1.2.1）
- **背景**：两项需求——跨设备通用优先于本机加密；覆盖安装后同步仍报 404。
- **改动**：
  1. `SiteRepository.kt`：移除 AES-256-GCM 加密，本地存储与云端上传恢复纯 base64（跨设备可直接解码）；
  2. 删除 `crypto/SiteCrypto.kt`，移除 `SecureStore.kt` 的 `KEY_SITES_CRYPTO`；
  3. `MainViewModel.kt`：`normalizeRepo()` 修复 `.git` 后缀剥离（此前仅 owner 剥离，repo 栏填完整 URL 时残留 `.git` 导致 Contents API 404）；
  4. `GitHubStorage.kt` & `MainViewModel.kt`：path 防呆处理，剥离前导斜杠。
- **404 根因**：v1.2.0 的 `normalizeRepo` 未对 repo 变量做 `.git` 剥离，API 路径变成 `repos/Aiyinsirui/PwdGen_sites_and_login.git/contents/...`——该仓库名不存在，GitHub 返回 404。
- **跨设备说明**：站点列表现为纯 base64（仅站点名+登录名，不含任何密码），换设备后可直接拉取并解码；主密码/生成密码仍永不落盘、永不上云。


### I. 删除确认、同步 404 修复、站点真正加密（最新）
- **背景**：四项需求——单条删除加确认弹窗、修复云端同步 404、site/login 真正加密存储、链式可回滚更新（尽力而为）。
- **改动**：
  1. `SitesScreen.kt`：单条删除按钮改为设置 `deleteTarget`，新增单条删除确认弹窗；
  2. `MainViewModel.kt`：新增 `normalizeRepo()` 归一化仓库地址（兼容用户把完整地址 `/Aiyinsirui/PwdGen_sites_and_login.git` 填进仓库名栏导致的 404）；
  3. `crypto/SiteCrypto.kt`：新建，AES-256-GCM，payload 前缀 `v2:`，格式 `base64(iv12 || ciphertext+tag)`；
  4. `SecureStore.kt`：新增 `KEY_SITES_CRYPTO` 常量，密钥经 Keystore 保护持久化；
  5. `SiteRepository.kt`：`load()/save()` 改用加密编解码，旧 base64 数据自动就地迁移加密；
  6. `MainViewModel.kt` 的 `syncNow()`：云端上传下载均走加密编解码。
- **重要取舍（跨设备须知）**：加密密钥存于本机 Keystore，**换设备后拉取的 v2 密文无法解密**（与原云端同步/跨设备诉求有冲突）。如需真正跨设备，需改为"随主密码派生密钥"方案。
- **说明**：GitHub Contents API 每次更新产生新 commit，仓库天然保留历史可回滚；App 内暂不做回滚 UI。

### H. 可选保存站点/登录名 + 历史键值对显示（最新）
- **背景**：用户希望生成结果能“可选保存 site/login”，并在历史列表中按键值对形式显示。
- **改动**：
  1. `Models.kt`：`SiteEntry` 新增 `login: String? = null` 字段；
  2. `SiteCodec.kt`：base64 编解码兼容 `site` 与 `site\tlogin` 两种格式，向后兼容旧数据；
  3. `SiteRepository.kt`：`add` 支持带 login 落库；`dedupe` 按 site 去重，同站点优先保留带 login 的条目；
  4. `SettingsRepository.kt`：新增 `saveSite`（默认开）/`saveLogin`（默认关）两个持久化键；
  5. `MainViewModel.kt`：生成落库逻辑遵循两个开关；新增对应 setter；
  6. `SettingsScreen.kt`：新增“记住站点”“同时记住登录名”两个开关；
  7. `SitesScreen.kt`：历史列表由仅显示 site 改为键值对显示——有 login 时显示 `[site:login]`，否则显示 site；
  8. 中英文 `strings.xml` 补充新增文案键。
- 说明：login 与 site 同存于 base64 明文块（base64 非加密），安全性等价现有云端存储模型，主密码与账号信息仍不落盘上云。

### G. 生成页输入顺序对齐 CLI（最新）
- **背景**：`pwdgen` 命令行工具的顺序是 `site → login → master password`，
  而 App 生成页原先是主密码在最前，与肌肉记忆不一致。
- **改动**：将生成页三个输入框重排为 **site → login → 主密码**，与 CLI 完全一致；
  键盘 IME 顺序同步为 Next → Next → Done（填完 login 直接跳主密码，最后 Done 收起键盘）。
- 计算逻辑、参数（长度/计数器/特殊字符）与结果卡片均未改动。

### F. 修复 ❌ 设过访问密码后进历史记录页不弹密码
- **现象**：设置站点访问密码后，再进入 Sites / 历史记录页仍直接放行，不要求输密码。
- **根因**：`MainViewModel.lockSites()` 早已存在，但**从未被任何地方调用**；而
  `setSitesLock()` / `changeSitesLock()` / `unlockSites()` 都会把 `sitesUnlocked`
  置为 `true`，会话内**没有任何逻辑把它复位为 `false`**。于是
  `SitesScreen` 的 `locked = sitesLockEnabled && !sitesUnlocked` 一旦解锁过就恒为
  false，页面永久放行。
- **修复**：在 `SitesScreen.kt` 增加
  `DisposableEffect(Unit) { onDispose { vm.lockSites() } }`——**离开该页面即重新上锁**，
  再次进入时必定重新弹出密码验证。改动仅 9 行，未触及图标与其它流程。

### A. 自定义应用图标
- 用**用户提供的照片**替换了默认的 Android Studio「小机器人」模板图标。
- 设计：品牌青绿→板岩蓝对角渐变底（`#1E6F5C → #2C5364`，取自 App 主题色）+ 居中的
  圆角照片徽章（带柔和投影），兼顾"用这张照片"的诉求与浅色照片在图标上的识别度。
- 覆盖全部密度桶（mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi），格式为无损 WebP：
  - `ic_launcher.webp` / `ic_launcher_round.webp`（legacy 圆角方形/圆形）
  - `ic_launcher_foreground.webp`（自适应图标前景层）
- 自适应图标 (`mipmap-anydpi-v26/`)：背景 = 渐变矢量，前景 = `@mipmap/ic_launcher_foreground`。
- 已移除机器人矢量 `drawable/ic_launcher_foreground.xml`。

### B. 站点列表访问密码（可选门禁）
Sites 页面可设访问密码，密码经
`EncryptedSharedPreferences(AES256_GCM)` 本地加密保存，**绝不上云**；锁定后进入该页
需先解锁，且顶栏不再显示站点数量以防泄漏。设置入口在「设置 → 安全」。

### C. 删除站点候选框
生成页站点输入框下方的自动补全下拉已移除，输入更清爽。

### D. 修复 ❌ 关不掉密码卡片
点击结果卡片右上角 ❌ 现在会正确关闭卡片
（改为调用 `MainViewModel.clearGenerated()`）。

### E. 访问密码鉴权修复
更改/移除站点访问密码现在必须校验当前密码后才生效。

> 算法约束未变：PBKDF2-HMAC-SHA256，10 万轮，salt=`site:login:counter`；
> 云端仅存 base64 的站点列表，主密码与账号信息永不落盘/上云。

---

## 二、APK 文件

| 文件 | 说明 | 大小 (字节) |
|---|---|---|
| `PwdGen-v1.2.1-release.apk` | **发布版（已签名）**，推荐分发 | 14,287,966 |
| `PwdGen-v1.2.1-debug.apk` | 调试版（Android debug 签名） | 21,410,143 |

> 注：本次为 `57f9e38` 构建（生成页输入重排），APK 体积与上一版一致（仅 UI 顺序调整）。

### SHA-256 校验
```
bfc24098dff756775092ada23cfc79b4c6afe51f7f6ee7ee509b229ff37980eb  PwdGen-v1.2.1-debug.apk
2a6559f9ecba6021b3e7ed939286cbdfe946100d9cf2e66e37b16b14668a81cc  PwdGen-v1.2.1-release.apk
89a2d07bdcdf216e6325ed71c9e1fc53fc16c6d7b1ab3e60c285c13ea9d22696  PwdGen-v1.0-debug.apk
fe4c94f0442b1c84ded80b3a77ca31c38f3b1726933153ec572269ab474cb11c  PwdGen-v1.0-source.tar.gz
```
（亦见 `SHA256SUMS.txt`）

### 签名信息
**Release（自定义 keystore = `pwdgen-release.jks`）**
- Signer DN：`CN=PwdGen, OU=Dev, O=PwdGen, L=City, ST=State, C=CN`
- SHA-256：`8bbc1a682f3a8d1de593866e8d31944086638a4725734fb0480c68c415428c11`
- SHA-1：`5b1d3e0c282e7e56a5fa28f2495d5328b5b88211`
- MD5：`5d709505bd24fb16efe42b9b4e942174`

**Debug（Android 默认调试签名）**
- Signer DN：`C=US, O=Android, CN=Android Debug`
- SHA-256：`a33909ec60329fc300b741172668cbf8f3e2bc250160431d0026ad65e222f36b`
- SHA-1：`ee9d04e4a4012d6e7c7ee5368b90e9af847f67bf`
- MD5：`3c32740abff3ebf4cc66b231357f84ea`

---

## 三、签名与构建配置

| 文件 | 用途 |
|---|---|
| `pwdgen-release.jks` | Release 签名 keystore |
| `keystore.properties` | 签名口令/别名（见下，**发布前务必更换**） |
| `gradle.properties` | 构建参数（含 ARM64 AAPT2 覆盖、关闭资源优化） |
| `build.gradle.kts` | 根构建脚本 |
| `settings.gradle.kts` | 模块与仓库声明 |
| `app-build.gradle.kts` | app 模块配置（签名/依赖/版本） |

### keystore.properties 内容（占位口令）
```
storeFile=pwdgen.jks
storePassword=pwdgen123
keyAlias=pwdgen
keyPassword=pwdgen123
```
> ⚠️ **正式发布前请务必更换 keystore 与口令**，并妥善离线保管 `pwdgen-release.jks`，
> 遗失将无法对已发布应用进行升级签名。
>
> 📌 注意：本包内的 keystore 文件名为 `pwdgen-release.jks`，但 `keystore.properties`
> 中写的是 `storeFile=pwdgen.jks`。若要直接用本目录复现 release 构建，请将
> `keystore.properties` 的 `storeFile` 改为 `pwdgen-release.jks`（或把 keystore
> 复制/重命名为 `pwdgen.jks` 并放到项目根目录）。

---

## 四、其他文件

| 文件 | 说明 |
|---|---|
| `PwdGen-v1.0-source.tar.gz` | 完整源码归档（app/src + 构建脚本 + keystore） |
| `README.md` | 项目说明 |
| `SHA256SUMS.txt` | APK 校验和 |
| `BUILD-INFO.md` | 本文件 |

---

## 五、构建环境说明（复现用）

- 环境：proot Ubuntu (ARM64)，`ANDROID_HOME=/root/Android`（build-tools 34/35/36）
- 必须使用 `--no-daemon`（proot 下否则 `pthread_create ENOSYS`）
- AAPT2 使用本地 ARM64 覆盖：`android.aapt2FromMavenOverride=<...>/tools/aapt2/linux-aarch64/aapt2`
- 已设 `android.enableResourceOptimizations=false`
- 构建命令：
  ```bash
  export ANDROID_HOME=/root/Android
  ./gradlew assembleDebug   --no-daemon   # ~50s
  ./gradlew assembleRelease --no-daemon   # ~1m43s
  ```

### 校验签名
```bash
apksigner verify --print-certs PwdGen-v1.0-release-signed.apk
```

### 校验完整性
```bash
sha256sum -c SHA256SUMS.txt
```

---

## 六、安装与使用

1. 分发**推荐使用** `PwdGen-v1.0-release-signed.apk`（体积小、已用正式密钥签名）。
2. `PwdGen-v1.0-debug.apk` 仅便于调试/对比，签名不同**不能覆盖安装** release 版
   （两者签名不同，需先卸载再装）。
3. 首次进入「设置 → 安全」可设置站点列表访问密码（可选，纯本地，不上云）。
4. 主密码与账号信息仅在本机内存/加密存储中使用，云端只保存 base64 站点列表。

---

## 七、文件清单速查

```
pwdgen_v2/
├── PwdGen-v1.0-release-signed.apk   # 发布版 APK（已签名，推荐分发）
├── PwdGen-v1.0-debug.apk            # 调试版 APK
├── pwdgen-release.jks               # Release 签名密钥库
├── keystore.properties              # 签名配置（占位口令，需更换）
├── gradle.properties                # 构建参数（ARM64 AAPT2 覆盖）
├── build.gradle.kts                 # 根构建脚本
├── settings.gradle.kts              # 模块/仓库声明
├── app-build.gradle.kts             # app 模块配置
├── PwdGen-v1.0-source.tar.gz        # 完整源码归档
├── README.md                        # 项目说明
├── SHA256SUMS.txt                   # APK 校验和
└── BUILD-INFO.md                    # 本说明文件
```

> 交付完成。本目录为自包含交付包，可整体分发；复现构建请参考第五节。
