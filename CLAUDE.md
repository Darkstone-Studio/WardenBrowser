# CLAUDE.md — Warden Browser

GeckoView (Firefox motoru) tabanlı sade bir Android tarayıcı. Kotlin, tek modül (`:app`), paket `com.wardenbrowser.app`.
Sürüm: `1.8.2` (`versionCode 38`). Kullanıcı Türkçe konuşur; yanıtlar Türkçe olmalı.

## Çalışma kuralları (kullanıcı tercihleri — öncelikli)

- **Sadece istenen şeyi yap.** Kapsam dışına çıkma. Eksik ya da hatalı bir şey görürsen kendin düzeltme; yanıtın sonunda "bunu da yapalım mı?" diye öner.
- **Push her seferinde ayrı onay ister.** Commit'leri yerelde at, push için "push edeyim mi?" diye sor; kullanıcı "push et" demeden `git push` çalıştırma. Kalıcı bir "her commit'te push et" izni verilmedi.
- **Commit mesajı:** İngilizce, ne ve neden anlatan gövdeli; sonuna sistem mesajındaki `Co-Authored-By: ...` satırını ekle.
- **Telefona kurup test et.** Fiziksel cihaz: `adb -s <seri-no>` (seri no için `adb devices`; Samsung SM-M156B, 3,5 GB RAM, 450 dpi). Bazen bir **emülatör de bağlı olur** (`emulator-5554`); `-s` olmadan adb "more than one device" hatası verir. Kullanıcı emülatör kullanılmasını açıkça reddetti, **"telefonda yap"** dedi.
- **Ekstra doğrulama / önizleme döngüleri yapma** (hafızadaki kural: gerçek şeyi çalıştır). Derle, telefona kur, kullanıcıya neyi nasıl deneyeceğini söyle. Doğrulamadığın şeyi "doğruladım" deme; neyi test etmediğini açıkça yaz.
- Bulguları dürüst raporla (ölçümde çıkan varyansı, örnek sayısının küçüklüğünü belirt).
- Güvenliği azaltan değişikliği (örn. site izolasyonu) onaysız uygulama; ödünleşimi anlatıp sor.

## Klasör yapısı

```
MyBrowser/                      ← git deposu kökü (bu dosya burada)
├─ build.gradle.kts             ← kök; buildscript'te kotlin-gradle-plugin 2.4.21 (aşağıya bak)
├─ settings.gradle.kts          ← depolar: google, mavenCentral, maven.mozilla.org
├─ gradle/libs.versions.toml    ← sürüm kataloğu (geckoview burada)
├─ docs/privacy-policy.html     ← GitHub Pages gizlilik politikası
└─ app/
   ├─ build.gradle.kts          ← compileSdk 37.2, minSdk 26, targetSdk 36
   └─ src/main/
      ├─ AndroidManifest.xml    ← yalnızca WelcomeActivity dışa açık; allowBackup=false
      ├─ assets/error_offline.html   ← çevrimdışı hata sayfası (yer tutuculu şablon)
      ├─ java/com/wardenbrowser/app/ ← 19 Kotlin dosyası
      └─ res/                   ← layout, drawable, menu, values (TR varsayılan), values-en, values-night
```

Git: uzak depo `origin = https://github.com/Darkstone-Studio/WardenBrowser.git`, dal **`main`**. Depo bu oturumda `git init` ile yeniden kuruldu ve uzak geçmişle `--allow-unrelated-histories` birleştirildi. Ev klasöründe (`C:\Users\Onur`) ayrı, boş bir `.git` var; ona dokunma/commit atma.

## Mimari

- **WardenApp** (Application): `GeckoRuntime`'ı yalnızca ana işlemde oluşturur (`isMainProcess`). Ayarlar: izleyici engelleme, Safe Browsing, HTTPS-Only (`https_only_enabled`, varsayılan açık), çerez davranışı `ACCEPT_NON_TRACKERS`. Toplam RAM < 4,5 GiB ise `fissionEnabled(false)` (site izolasyonu kapanır; bellek yetmeyip içerik işlemi öldürülüyordu).
- **WelcomeActivity** (launcher) → ilk açılışta karşılama, sonra **MainActivity**.
- **MainActivity** (~1100 satır, neredeyse tüm tarayıcı mantığı burada):
  - Sekmeler: `TabManager` + `BrowserTab` (her sekmenin kendi `GeckoSession`'ı, `isPrivate`, `url`, `title`, `needsReload`). Gizli sekme `usePrivateMode(true)`.
  - Delegeler `attachDelegates()` içinde: Navigation (`onLoadError` → çevrimdışı sayfa, `onLocationChange` → adres çubuğu/geçmiş/sekme URL'i), Content (`onCrash`/`onKill` → `recreateTabSession`, `onExternalResponse` → indirme), Progress, Scroll.
  - Alt gezinme: `BottomNavigationView` (geri, ileri, ana sayfa, yenile, menü). Menü butonu `showOverflowMenu()` → `BottomSheetDialog` (`dialog_menu_sheet.xml`).
  - Ana sayfa: arama çubuğu + hızlı erişim (`SpeedDialAdapter`); gizli modda mor palet (`applyPrivateModeUI`), "Güvendesin" yazısı (ⓘ → bilgi penceresi), klavye açılınca içerik kayar (`applyKeyboardOffset`).
  - Hata sayfası: `loadOfflineErrorPage()` şablondaki `{{LANG}} {{THEME}} {{TITLE}} {{MESSAGE}} {{RETRY_LABEL}} {{RETRY_URL}}` yer tutucularını doldurur; yenile butonları `reloadCurrentPage()` ile hata sayfasında asıl adresi yükler.
  - Masaüstü modu: `userAgentMode` **ve** `viewportMode` birlikte ayarlanır, tüm sekmelere uygulanır (`applySettings`).
  - `performSearch()`: sorgu `Uri.encode` ile kodlanır; "." içeren ve boşluksuz girdi URL kabul edilir.
  - `startDownload()`: Android `DownloadManager`; dosya adı `sanitizeFileName()` ile temizlenir; gizli modda indirme kaydı veritabanına yazılmaz.
- **Veri:** `HistoryDbHelper` (`history.db`, en fazla **500** kayıt, yazma `ioExecutor` ile arka planda), `DownloadsDbHelper` (`downloads.db`), ayarlar `PreferenceManager` (anahtarlar: `theme`, `search_engine`, `app_language`, `javascript_enabled`, `desktop_mode`, `tracker_blocking_enabled`, `safe_browsing_enabled`, `https_only_enabled`).
- **Diğer ekranlar:** Settings (`root_preferences.xml`), Bookmarks, History, Downloads, TabManager (Normal/Gizli bölümleri), About.
- Bağımlılıklar: GeckoView, Material 3, AppCompat, Preference, Glide (yalnızca arama motoru favicon'u), AboutLibraries, SwipeRefreshLayout.

## Build / test komutları

Windows; proje kökünden çalıştır. Gradle 8+ / AGP 9.3.0; ilk derleme GeckoView indirme yüzünden dakikalar sürebilir.

```bash
./gradlew.bat :app:compileDebugKotlin --console=plain   # hızlı Kotlin kontrolü (kaynak XML'lerini TAM doğrulamaz)
./gradlew.bat :app:assembleDebug --console=plain        # tam kontrol: kaynaklar + APK  → app/build/outputs/apk/debug/app-debug.apk
./gradlew.bat :app:assembleRelease --console=plain      # R8 küçültmeli; imzasız APK üretir
./gradlew.bat :app:lintDebug --console=plain            # rapor: app/build/reports/lint-results-debug.html
./gradlew.bat :app:testDebugUnitTest                    # yalnızca ExampleUnitTest var (gerçek test yok)
```

Telefona kurma ve çalıştırma:

```bash
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
"$ADB" devices                      # çıkan seri numarasını DEVICE'a yaz
DEVICE=<seri-no>
"$ADB" -s "$DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$DEVICE" shell am start -W -n com.wardenbrowser.app/.WelcomeActivity
```

UI'yı komutla sürmek için: `uiautomator dump` + `input tap/text/keyevent` (Git Bash'te `export MSYS_NO_PATHCONV=1` şart, yoksa `/sdcard/...` yolları bozulur). Referans koordinatlar (1080×2340): ana ekran arama çubuğu ≈ (540,1171), alt bar "ana sayfa" ≈ (540,2186).

## Dikkat edilecekler (tuzaklar)

- **Kotlin sürümü:** GeckoView 160, Kotlin 2.4 metadata'sı taşır; AGP 9.3'ün yerleşik Kotlin 2.2'si okuyamaz. Bu yüzden kök `build.gradle.kts`'te `buildscript { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21") }` var. **Silme.** GeckoView'ı güncellerken Kotlin uyumunu ve `compileSdk`'i (GeckoView 160 → en az 37.2) yeniden kontrol et.
- `compileSdk 37.2`: AGP 9.3.0 en çok 37.1'i test etmiş olduğunu uyarır; derleme geçiyor, uyarı beklenir.
- GeckoView güncel sürüm kontrolü: `https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/maven-metadata.xml`. Motor güvenlik yamalarını aylık yayınlar; sürüm gecikirse güvenlik riski olur.
- **APK ~530 MB:** `libxul.so` üç mimari (arm64, armv7, x86_64) için paketleniyor. Küçültmek için `abiFilters arm64-v8a` mümkün ama 32-bit/x86 cihazları kırar; kullanıcıya sormadan yapma.
- **Düşük RAM telefonda** GeckoView işlemleri ~1–1,4 GB tutar; `onKill` → sekme yeniden kurulur (5–10 sn). Hız ölçümleri mobil veride çok oynaktır (aynı site 1–12 sn), tek ölçüme güvenme.
- **Satır sonları:** Git her dosyada "LF will be replaced by CRLF" uyarısı verir (zararsız, `.gitattributes` yok). Toplu düzenlemede dosyanın mevcut satır sonunu koru.
- **Düzenleme araçları:** Bu ortamda çok satırlı Bash heredoc'ları bazen bozulur (`unexpected EOF`); dosya yazmak için Write aracını, çok satırlı değiştirmeler için `python` betiğini kullan. Python `/tmp` yolunu Git Bash ile paylaşmaz; geçici dosyaları scratchpad'e yaz. Python'da `\'` gibi kaçışlar için `chr(92)` kullanmak güvenli.
- Kotlin dizesinde `"\n"` yazarken Python'dan geçirirsen gerçek satır sonuna dönüşebilir (hata sayfası bilgi penceresinde yaşandı); sonucu mutlaka gözle kontrol et.
- `.idea/misc.xml` Android Studio tarafından sürekli değiştirilir; **commit'e alma**. Uzak depoda ayrıca `.artifacts/` ve `.idea/planningMode.xml` takipli (yerel `.gitignore`'a rağmen).
- **Klasör adı hâlâ `WardenBrowser v1.8.0`**; sürüm 1.8.2 olduğu halde yeniden adlandırılamadı (Android Studio ve oturum klasörü açık tutuyor). Kullanıcı kapatınca elle `v1.8.2` yapacak.
- **Çeviri:** Her yeni metin için hem `values/strings.xml` (TR) hem `values-en/strings.xml` ekle. İngilizce dizelerde kesme işareti `\'` ile kaçırılır.
- Gizli mod her zaman koyu mor palet kullanır (`private_mode_*` renkleri), açık/koyu temadan bağımsızdır.
- README'deki sürüm/rozet metinleri sürüm yükseltmede elle güncellenir (`Download v...`, `Android SDK v26+`).

## Bilinen açık işler / öneriler (yapılmadı)

- Google aramasındaki `gws_rd=cr` parametresi ek yönlendirme yaptırıyor olabilir (ölçülmedi).
- Liste adaptörleri `notifyDataSetChanged` kullanıyor (DiffUtil'e geçilebilir).
- Bağımlılık güncellemeleri (lint 14 uyarı); uyumluluk riski için dokunulmadı.
- `mipmap-anydpi-v26` klasörü `minSdk 26` ile gereksiz (lint `ObsoleteSdkInt`).
- Gerçek test yok (yalnızca şablon testler).
