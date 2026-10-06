# Warden Browser (Beta)

Mozilla GeckoView tabanlı, sade ve performans odaklı bir Android web tarayıcısı.

Warden Browser, gereksiz eklentilerden uzak durup temel tarayıcı deneyimini hızlı ve akıcı şekilde sunmayı hedefler. Chromium WebView yerine GeckoView (Firefox motoru) kullanır.

---

## Özellikler

- GeckoView (Firefox motoru) tabanlı hızlı sayfa render
- Adres çubuğundan URL veya arama sorgusu girme, seçilebilir arama motoru (Google, DuckDuckGo, Brave, Bing)
- Hızlı erişim (speed dial) ana ekranı
- Gezinme geçmişi kaydı ve tek tuşla temizleme
- Gizli sekme modu — geçmiş kaydedilmez, ayrı oturum
- JavaScript açma/kapama, masaüstü modu, tema seçimi (açık/koyu/sistem)
- Material Design arayüz, kenardan kenara (edge-to-edge) tasarım

---

## Kurulum

### Android Studio ile

```bash
git clone https://github.com/Darkstone-Studio/WardenBrowser.git
```

Projeyi Android Studio'da açın, Gradle sync tamamlandıktan sonra çalıştırın.
Minimum SDK 26 (Android 8.0) gerektirir.

### Google Play Store (Beta)

Warden Browser şu an Google Play Store'da açık beta aşamasındadır. Uygulamayı doğrudan Play Store üzerinden indirip test edebilirsiniz:

[![Get it on Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=com.wardenbrowser.app)

### APK

Derlenmiş APK dosyaları **Releases** bölümünden indirilebilir. Play Store veya Galaxy Store dışından kurulum yaptığınız için cihazınız "bilinmeyen kaynak" uyarısı gösterebilir — bu normaldir.

**İndir:** [Download v1.7.0](https://github.com/Darkstone-Studio/WardenBrowser/releases/latest)

**En son sürüm:** `v1.7.0`

---

## Kullanılan Teknolojiler

![Kotlin](https://img.shields.io/badge/Kotlin-Native-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Android SDK](https://img.shields.io/badge/Android_SDK-v21+-3DDC84?style=flat-square&logo=android&logoColor=white)
![GeckoView](https://img.shields.io/badge/Mozilla_GeckoView-Engine-FF7139?style=flat-square&logo=firefoxbrowser&logoColor=white)
![Material Design](https://img.shields.io/badge/Material_Components-UI-757575?style=flat-square&logo=materialdesign&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-History-003B57?style=flat-square&logo=sqlite&logoColor=white)

| Teknoloji | Rol |
|---|---|
| **Kotlin** | Ana uygulama dili |
| **Android SDK** | Yerel uygulama geliştirme altyapısı |
| **Mozilla GeckoView** | Tarayıcı motoru (Web Engine) |
| **Material Components** | Modern UI ve tema bileşenleri |
| **SQLite** | Gezinme geçmişi ve yerel veri depolama |

---

## Yol Haritası

- [x] Gezinme geçmişi
- [x] Gizli sekme modu
- [x] Çoklu sekme desteği
- [ ] Yer imleri (henüz kararlı değil)
- [ ] İzleyici/reklam engelleme (geliştirme aşamasında, henüz kararlı değil)
- [ ] İndirme yöneticisi (geliştirme aşamasında, henüz kararlı değil)

## Bilinen Kısıtlar

Proje aktif geliştirme aşamasında. Yer imleri henüz işlevsel değil, izleyici engelleme özelliği test edilip stabilite sorunları nedeniyle geri alındı.

---

## Güvenlik & Gizlilik

[Gizlilik Politikası](https://darkstone-studio.github.io/WardenBrowser/privacy-policy.html)

---

## Katkıda Bulunma

Katkılar, hata bildirimleri ve öneriler memnuniyetle karşılanır.

1. Fork oluşturun.
2. Yeni bir branch açın.
3. Değişikliklerinizi yapın.
4. Pull Request gönderin.

---

## İletişim

Öneri, talep veya sorularınız için: **darkstonestudio.dev@gmail.com**

Ayrıca [GitHub Issues](https://github.com/Darkstone-Studio/WardenBrowser/issues) üzerinden de ulaşabilirsiniz.

---

## License

Bu proje [PolyForm Noncommercial License 1.0.0](LICENSE.md) ile lisanslanmıştır.
Ticari kullanım (satış, yeniden satış veya ücretli dağıtım dahil) izin verilmemektedir.
