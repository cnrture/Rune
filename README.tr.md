# Rune

![Build](https://github.com/cnrture/Rune/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

Turkce | **[English](README.md)**

**Claude CLI**'yi JetBrains IDE'nize entegre edin. Rune; yapay zeka destekli commit mesajlari, PR yonetimi, kod sorgulama ve genisletilebilir skill/agent sistemi sunar — tek bir tool window uzerinden.

## Ozellikler

### Claude Terminal

IDE'nin sag panelinde gomulu Claude CLI oturumlari.

- **Coklu oturum** — Birden fazla Claude terminali acip aralarinda gecis yapabilirsiniz
- **Model secimi** — Opus, Sonnet ve Haiku modelleri arasinda hizli gecis
- **Komut paleti** — `/` yazarak skills, agents, Claude komutlari ve SuperClaude komutlarina erisim
- **Dosya enjeksiyonu** — `@` butonuyla acik dosyanin yolunu mesaja ekler
- **Gorsel gonderimi** — Mesajla birlikte gorsel secip gonderebilirsiniz
- **Remote control** — Uzaktan Claude oturumu baslatma, opsiyonel `caffeinate` ile uyku engelleme (macOS)

---

### Commit Mesaji Olusturma

VCS commit diyalogundaki **"Generate Commit Message with Claude"** butonu.

- Stage'lenmis ve stage'lenmemis degisikliklerden conventional commit mesaji uretir
- Ozellestirilebilir prompt sablonu
- Opsiyonel Jira entegrasyonu — branch isminden ticket ID cikarilip commit mesajina URL eklenir
- Claude bulunamazsa fallback mantigi — dosya uzantilarini ve degisiklikleri analiz ederek tip belirler

---

### PR Olusturma ve Yonetim

**"Create Review PR"** butonu — tek tikla pull request olusturun.

- Base branch otomatik tespiti (develop, main, master, staging, release)
- Opsiyonel `review/` branch prefix'i
- Repo contributor'larindan aranabilir reviewer secimi
- Label secimi — mevcutlardan secim veya yeni olusturma
- Jira entegrasyonu — ticket Fix Version'a gore otomatik label secimi
- **GitHub (`gh` CLI) ve Bitbucket Cloud (REST API) destegi**

---

### PR Yorum Duzeltme

**"Fix PR Comments"** butonu — cozulmemis review yorumlarini Claude ile duzeltir.

- GitHub GraphQL API veya Bitbucket REST API ile cozulmemis review thread'leri cekilir
- Yorumlar dosya yolu, satir numarasi, reviewer ismi ve kod context'i ile listelenir
- Istenen yorumlar secilip formatlanarak Claude terminaline gonderilir

---

### Ask Claude

Editorde kod secip sag tik menusuyle **"Ask Claude"** secenegini kullanin.

- Secili kod dosya yolu ve satir numarasi bilgisiyle Claude'a gonderilir
- Claude tool window'u otomatik acilir
- Console ciktisi icin de calisir

---

### Skill ve Agent Sistemi

Markdown dosyalari olarak tanimlanan ozel skill ve agent'lar, plugin tarafindan otomatik kesfedilir.

- **Skills** — Konfigure edilen dizindeki `SKILL.md` uzantili dosyalar
- **Agents** — Konfigure edilen dizindeki herhangi bir `.md` dosyasi
- **Skill olusturma diyalogu** — Isim, aciklama, workflow, ornekler ve referanslar tanimlayabilirsiniz
- **Dogrulama** — Isim formati, aciklama kalitesi ve frontmatter yapisi kontrol edilir
- **Best practices kontrolu** — SKILL.md dosyalarinda "Check with Claude" aksiyonuyla bildirim banner'i

---

### Komut Paleti

Terminal giris alaninda `/` yazarak veya palet butonuna tiklayarak acilir. 4 kategoride aranabilir icerik:

| Kategori    | Kaynak                                                        |
|-------------|---------------------------------------------------------------|
| Skills      | Skills dizinindeki SKILL.md dosyalari                         |
| Agents      | Agents dizinindeki .md dosyalari                              |
| Commands    | 29 yerlesik Claude komutu (`/clear`, `/model`, `/compact`...) |
| SC Commands | 25+ SuperClaude komutu (`/sc:analyze`, `/sc:design`...)       |

---

## Ayarlar

**Settings > Tools > Rune Settings**

| Ayar                   | Aciklama                                                             |
|------------------------|----------------------------------------------------------------------|
| Skills Directory       | Skill dosyalarinin taranacagi dizin (varsayilan: `.claude/skills`)   |
| Agents Directory       | Agent dosyalarinin taranacagi dizin (varsayilan: `.claude/agents`)   |
| Commit Message Prompt  | Claude'a gonderilen commit prompt sablonu (ozellestirilebilir)       |
| Jira Ticket URL        | Commit mesajina Jira ticket linki ekleme                             |
| Review Branch          | PR icin `review/` branch prefix'i kullanma                          |
| VCS Provider           | GitHub veya Bitbucket Cloud (git remote'dan otomatik tespit edilir)  |
| GitHub Credentials     | GitHub token (IDE credential store'da guvenli saklanir)              |
| Bitbucket Credentials  | Bitbucket kullanici adi ve API token                                 |
| Jira Credentials       | Jira email ve API token                                              |

---

## Kurulum

1. IDE'de <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> acin
2. `"Rune"` arayin ve <kbd>Install</kbd> tiklayin

## Gereksinimler

- JetBrains IDE (IntelliJ IDEA, Android Studio, WebStorm, PyCharm, GoLand vb.) **2024.1+**
- [Claude CLI](https://docs.anthropic.com/en/docs/claude-code) kurulu ve PATH'te erisilebilir olmali
- [GitHub CLI](https://cli.github.com/) (`gh`) — GitHub PR islemleri icin gerekli
- Java 21+

## Gelistirme

```bash
# Plugin'i sandbox IDE'de calistir
./gradlew runIde

# Testleri calistir
./gradlew check

# Dagitim ZIP'i olustur
./gradlew buildPlugin

# Plugin uyumluluk dogrulamasi
./gradlew runPluginVerifier
```

**Tech stack:** Kotlin 2.3.10 · Jetpack Compose Desktop 1.10.1 · IntelliJ Platform SDK · FreeMarker 2.3.34

---

Plugin [IntelliJ Platform Plugin Template][template] uzerine kurulmustur.

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
