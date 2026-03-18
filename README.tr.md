# 🤖 Rune

![Build](https://github.com/cnrture/Rune/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

🌐 Türkçe | **[English](README.md)**

JetBrains IDE'leri icin AI destekli gelistirme asistani. Claude CLI entegrasyonu, otomatik commit mesaji olusturma, PR yonetimi, skill/agent sistemi ve daha fazlasi. IntelliJ IDEA, Android Studio, WebStorm, PyCharm ve diger tum JetBrains IDE'lerinde calisir.

## ✨ Ozellikler

### 🖥️ Claude Terminal Entegrasyonu

Plugin, IDE'nin sag paneline **Claude** tool window'u ekler. Bu panel icinde Claude CLI dogrudan calisir.

- 📑 **Coklu oturum destegi** — Birden fazla Claude terminali acip aralarinda gecis yapabilirsiniz
- 🎯 **Komut paleti** — `/` yazarak veya butona tiklayarak skills, agents, Claude komutlari ve SuperClaude komutlarina erisim
- 📎 **Dosya enjeksiyonu** — `@` butonuyla acik dosyanin yolunu mesaja ekler
- 🖼️ **Gorsel gonderimi** — Mesajla birlikte gorsel secip gonderebilirsiniz
- 🌐 **Remote control** — Uzaktan Claude oturumu baslatma, opsiyonel `caffeinate` ile uyku engelleme (macOS)

> **⚙️ Nasil calisir:** `ClaudeSessionService` oturumlari yonetir. Terminal, IntelliJ'in native `JBTerminalWidget`'ini kullanir. Claude CLI, PATH uzerinden bulunur; bulunamazsa `~/.npm-global/bin`, `~/.local/bin`, `/usr/local/bin` gibi bilinen konumlarda aranir.

---

### 💬 Otomatik Commit Mesaji Olusturma

VCS commit diyalogundaki **"Generate Commit Message with Claude"** butonu, stage'lenmis degisikliklerden commit mesaji uretir.

- 📝 `git diff --cached` ve `git diff` ciktilari Claude CLI'a gonderilir
- 📏 Conventional commit formatinda mesaj uretilir
- 🔗 Opsiyonel Jira entegrasyonu: branch isminden ticket ID cikarilip commit mesajina URL eklenir
- 🔄 Claude bulunamazsa fallback mantigi devreye girer (dosya uzantilarini ve degisiklikleri analiz ederek tip belirler)

> **⚙️ Nasil calisir:** `GenerateCommitMessageAction`, `claude -p <prompt>` komutunu 30 saniyelik timeout ile calistirir. Ciktiyi commit mesaji alanina stream eder.

---

### 🚀 PR Olusturma

**"Create Review PR"** butonu ile tek tikla pull request olusturabilirsiniz.

- 🌿 Base branch otomatik tespit edilir (git reflog veya en yakin ata branch: develop, main, master, staging, release)
- 🏷️ Opsiyonel `review/` branch prefix'i
- 👥 Reviewer secimi — repo contributor'larindan aranabilir liste
- 🏷️ Label secimi — mevcut label'lardan secim veya yeni label olusturma
- 🎫 Jira entegrasyonu: ticket Fix Version'a gore otomatik label secimi

> **⚙️ Nasil calisir:** `CreateReviewPRAction` once base branch'i tespit eder, branch'i push eder, sonra `CreatePRDialog`'u acar. PR, GitHub CLI (`gh pr create`) ile olusturulur. Collaborator ve label verileri `GitHubCacheService` ile onbelleklenir.

---

### 🔧 PR Yorum Duzeltme

**"Fix PR Comments"** butonu, cozulmemis PR review yorumlarini Claude ile duzeltir.

- 🔗 PR URL'si girilir
- 📡 GitHub GraphQL API ile cozulmemis review thread'leri cekilir
- 📋 Yorumlar dosya yolu, satir numarasi, reviewer ismi ve kod context'i ile listelenir
- ✅ Istenen yorumlar secilip Claude'a gonderilir

> **⚙️ Nasil calisir:** `FixPRCommentsAction`, GraphQL sorgusuyla yorum verilerini ceker. `FixPRCommentsDialog` yorumlari gosterir. Secilen yorumlar formatlanip Claude terminaline paste edilir.

---

### 💡 Ask Claude (Kod Sorgulama)

Editorde kod secip sag tik menusuyle **"Ask Claude"** secenegini kullanabilirsiniz.

- 📄 Secili kod + dosya yolu ve satir numarasi bilgisiyle Claude'a gonderilir
- 🪟 Claude tool window'u otomatik acilir
- 🖥️ Console ciktisi icin de calisir

> **⚙️ Nasil calisir:** `AskClaudeAction`, secili metni ve dosya context'ini `ClaudeSessionService.setPendingInput()` ile terminal giris alanina enjekte eder.

---

### 🧩 Skill ve Agent Sistemi

Skills ve agents, Markdown dosyalari olarak tanimlanir ve plugin tarafindan otomatik kesfedilir.

- 📘 **Skills** — `SKILL.md` uzantili dosyalar, belirli bir dizinden taranir
- 🤖 **Agents** — Herhangi bir `.md` dosyasi, ayri bir dizinden taranir
- ✏️ **Skill olusturma** — `CreateSkillDialog` ile isim, aciklama, workflow, ornekler ve referanslar tanimlayabilirsiniz
- ✅ **Dogrulama** — Isim formati, aciklama kalitesi, frontmatter yapisi kontrol edilir

> **⚙️ Nasil calisir:** `SkillRepositoryImpl`, konfigure edilen dizini recursive olarak tarar. 5 dakikalik onbellek kullanir. Dosyanin ilk bos olmayan satiri aciklama olarak alinir.

---

### 📋 Skill Best Practices Kontrolu

`SKILL.md` dosyasi acildiginda editorde bildirim banner'i gosterilir.

- 📖 **"Open best practices"** — Claude dokumantasyonunu tarayicida acar
- 🔍 **"Check with Claude"** — Skill icerigini Claude'a gonderip detayli review yaptirir (frontmatter, isimlendirme, yapi, icerik kalitesi)

---

### 🎨 Komut Paleti

Terminal giris alaninda `/` yazarak veya butona tiklayarak acilir. 4 kategoride aranabilir icerikleri listeler:

| Kategori | Kaynak |
|---|---|
| 📘 Skills | Skills dizinindeki SKILL.md dosyalari |
| 🤖 Agents | Agents dizinindeki .md dosyalari |
| ⌨️ Commands | 29 yerlesik Claude komutu (`/clear`, `/model`, `/compact`...) |
| ⚡ SC Commands | 25 SuperClaude komutu (`/sc:analyze`, `/sc:design`...) |

---

## ⚙️ Ayarlar

**Settings > Tools > Rune Settings** yolundan yapilandirilir.

| Ayar | Aciklama |
|---|---|
| 📁 Skills Directory | Skill dosyalarinin taranacagi dizin |
| 📁 Agents Directory | Agent dosyalarinin taranacagi dizin |
| 💬 Commit Message Prompt | Claude'a gonderilen commit prompt sablonu (ozellestirilebilir) |
| 🔗 Jira Ticket URL | Commit mesajina Jira ticket linki ekleme |
| 🌿 Review Branch | PR icin `review/` branch prefix'i kullanma |
| 🔑 Jira Credentials | Jira email ve API token (IDE credential store'da guvenli saklanir) |

---

## 📦 Kurulum

1. IDE'de <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Manage Plugin Repositories...</kbd> acin
2. Asagidaki URL'yi ekleyin:
   ```
   https://raw.githubusercontent.com/cnrture/rune-releases/main/updatePlugins.xml
   ```
3. <kbd>Marketplace</kbd> sekmesinde `"Rune"` arayin ve <kbd>Install</kbd> tiklayin

## 📋 Gereksinimler

- 🧰 JetBrains IDE (IntelliJ IDEA, Android Studio, WebStorm, PyCharm, GoLand vb.) 2024.1+
- 🤖 [Claude CLI](https://docs.anthropic.com/en/docs/claude-code) kurulu ve PATH'te eriselebilir olmali
- 🐙 [GitHub CLI](https://cli.github.com/) (`gh`) — PR islemleri icin
- ☕ Java 21+

## 🛠️ Gelistirme

```bash
# Plugin'i sandbox IDE'de calistir
./gradlew runIde

# Testleri calistir
./gradlew check

# Dagitim ZIP'i olustur
./gradlew buildPlugin
```

**Tech stack:** Kotlin 2.3.0 · Jetpack Compose Desktop 1.10.1 · IntelliJ Platform SDK · FreeMarker

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
