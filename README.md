# PwdGen

🔐 **离线、确定性密码生成器 for Android**

> PwdGen 根据「主密码 + 站点 + 账号 + 计数器」用 **PBKDF2-HMAC-SHA256（100,000 轮）** 确定性地派生出站点密码。
> 你永远不需要把密码存到任何地方 —— 只要记住主密码，就能在任何设备上重新算出同样的密码。

---

## ✨ 特性

- 🔑 **确定性生成**：同样输入永远得到同样密码，无需保存密码本身
- 🛡️ **完全离线**：主密码与账号信息**绝不落盘、绝不上云**
- 🌐 **可选云同步**：仅同步 base64 编码的**站点列表**（不含任何密码），走你自己的 GitHub 仓库
- 🔒 **站点列表门禁**：可选「访问密码」保护站点列表，修改/移除**必须验证当前密码**
- 🎨 **Material 3 + Jetpack Compose**：动态取色、深浅色主题、自定义壁纸
- 🇨🇳 **中英双语**（跟随系统 / 手动切换）

---

## 🔐 安全模型

| 数据 | 是否落盘 | 是否上云 | 加密方式 |
|---|---|---|---|
| 主密码 | ❌ 不保存（可选本机记住） | ❌ 绝不上云 | — |
| 生成的账号密码 | ❌ 不保存 | ❌ 绝不上云 | — |
| 站点列表（名称/账号/计数器等元数据） | ✅ 本地 | ✅ 仅 base64 | — |
| 访问密码 | ✅ 本地 | ❌ 绝不上云 | `EncryptedSharedPreferences` (AES256-GCM) |
| 「记住主密码」（可选） | ✅ 本地 | ❌ 绝不上云 | `EncryptedSharedPreferences` |

### 派生算法

```
password = Base64( PBKDF2-HMAC-SHA256(
    password  = masterPassword,
    salt      = "site:login:counter",
    iterations= 100000,
    keyLength = 32
) )  // 按所选规则裁剪字符集 / 长度
```

> 同 `masterPassword` + `site` + `login` + `counter` ⇒ 永远得到同一个密码。

---

## 📸 功能一览

- **生成页**：输入站点 / 账号 / 计数器 → 一键生成，可复制、可调整长度与字符集
- **站点页**：保存站点条目，快速取回；受访问密码门禁保护
- **备份/还原**：导出站点列表为 base64，可粘贴到任意设备还原
- **设置页**：语言、主题、动态取色、壁纸；访问密码设置/修改/移除；GitHub 云同步

---

## 🛠️ 构建

### 环境要求
- JDK 17+
- Android SDK（build-tools 34+）
- Gradle（已内置 Wrapper）

### 命令行

```bash
# Debug
./gradlew assembleDebug

# Release（需签名配置，见下）
./gradlew assembleRelease
```

产物位置：
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

### 签名配置（Release）

复制示例文件并填入你自己的密钥信息：

```bash
cp keystore.properties.example keystore.properties
# 然后编辑 keystore.properties，填入 storeFile / storePassword / keyAlias / keyPassword
```

> ⚠️ `keystore.properties` 与 `*.jks` 已被 `.gitignore` 忽略，**切勿提交真实口令与密钥**。

---

## 📁 项目结构

```
app/src/main/java/com/pwdgen/app/
├── MainActivity.kt
├── core/            # 生成算法、加密存储
├── data/            # 数据模型、仓库、云同步
└── ui/
    ├── MainViewModel.kt
    ├── theme/
    └── screens/     # GenerateScreen / SitesScreen / SettingsScreen ...
```

---

## 🧩 技术栈

- Kotlin · Jetpack Compose · Material 3
- AndroidX Security（`EncryptedSharedPreferences`）
- Gradle Version Catalog · Kotlin DSL

---

## 📄 许可

本项目仅供个人学习与使用。请妥善保管你的主密码 —— **一旦丢失，无法找回，也无法恢复任何派生密码。**

---

## ⚠️ 免责声明

PwdGen 生成的所有密码均由你的主密码在本地计算得出，开发者不接触、不存储、不传输你的任何密码或个人数据。
---
## 📝 变更日志

### v1.2.1（2026-09-29）
- 🛠️ **站点列表存储/同步回退为纯 base64**（撤销 v1.2.0 的 AES-256-GCM 本机加密），恢复跨设备通用能力——换设备后可直接解密云端站点列表
- 🐛 **修复 GitHub 同步 404**：仓库名栏填完整 URL（如 `https://github.com/owner/repo.git`）时，`.git` 后缀未剥离导致 Contents API 路径错误；现已正确归一化 owner/repo，并防呆处理文件路径前导斜杠
### v1.2.0（2026-09-28）
- ✅ 单条站点删除增加确认弹窗
- 🐛 尝试修复云端同步 404（repo 归一化）
- ❌ 引入了 AES 加密，后因跨设备冲突在 v1.2.1 回退
### v1.1.0（2026-09-26）
- ➕ 新增可选保存站点 + 登录名字段，历史列表支持键值对显示
- ➕ 生成页输入框顺序重排（site → login → 主密码）
- 🎨 自定义应用图标、离开历史页自动重新上锁
### v1.0.0
- 初始版本
