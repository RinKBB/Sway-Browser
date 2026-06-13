<div align="center">

# 🌊 Sway Browser

[![GitHub Release](https://img.shields.io/github/v/release/RinKBB/Sway-Browser?label=Release&color=brightgreen)](https://github.com/RinKBB/Sway-Browser/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Android API](https://img.shields.io/badge/Android-API%2024+-blue.svg)](https://www.android.com/)
[![Material Design 3](https://img.shields.io/badge/Design-Material%203-teal.svg)](https://m3.material.io/)

**Современный Android-браузер с интеллектуальным загрузчиком медиафайлов**

[📥 Скачать APK](#installation) • [🏗️ Архитектура](#architecture) • [🛠️ Стек технологий](#tech-stack) • [🎯 Функционал](#features) • [🐛 Ограничения](#limitations) • [📞 Помощь](#support)

</div>

---

## 📋 Содержание

1. [О проекте](#about)
2. [Быстрый старт](#quick-start)
3. [Технический стек](#tech-stack)
4. [Архитектура](#architecture)
5. [Основной функционал](#features)
6. [История логотипа](#logo-story)
7. [Установка](#installation)
8. [Локальная разработка](#development)
9. [Известные ограничения](#limitations)
10. [Поддержка и помощь](#support)
11. [Лицензия](#license)

---

<a id="about"></a>
## 📱 О проекте

**Sway Browser** — это полнофункциональный мобильный браузер для Android, разработанный на современном стеке Jetpack Compose и Material Design 3. Приложение создано для любителей веб-технологий и экспериментировать с новыми подходами в разработке мобильных приложений.

Основные возможности:

- ✨ **Элегантный интерфейс** на Material Design 3
- 🎯 **Интеллектуальная система обнаружения** медиафайлов на веб-страницах
- 📥 **Мощный менеджер загрузок** с поддержкой ZIP-архивов
- 🌍 **Кастомный поиск изображений** через Unsplash API (с fallback на локальную БД)
- 🔒 **Встроенные функции приватности** (блокировка трекеров, очистка истории)
- 🎨 **Поддержка нескольких языков** (Русский 🇷🇺, Английский 🇬🇧, Казахский 🇰🇿)
- 🗂️ **Управление вкладками, закладками и историей**

> **Примечание:** Это учебный проект, создан для демонстрации лучших практик Android-разработки.

---

<a id="quick-start"></a>
## 🚀 Быстрый старт

### Для пользователей
```bash
# 1. Перейдите на GitHub Releases
# https://github.com/RinKBB/Sway-Browser/releases

# 2. Скачайте последний APK
# 3. Установите на Android-устройство (API 24+)
```

### Для разработчиков
```bash
git clone https://github.com/RinKBB/Sway-Browser.git
cd Sway-Browser
# Откройте в Android Studio
# Создайте .env файл с API ключом (см. Development раздел)
# Запустите приложение
```

---

<a id="tech-stack"></a>
## 🛠️ Технический стек

### Core Framework & Build
- **Kotlin** 2.2.10 — язык разработки
- **Android Gradle Plugin** 9.1.1 — система сборки
- **Java** 11 — целевая версия JVM
- **Gradle** 8.x — управление зависимостями

### Jetpack & UI
- **Jetpack Compose** 2024.09.00 — современный декларативный UI
- **Material 3** — Material Design система компонентов
- **Material Icons** (Core & Extended) — иконографический пакет
- **Coil** 2.7.0 — асинхронная загрузка и кеширование изображений
- **Activity Compose** 1.10.1 — интеграция Compose с Activity

### Lifecycle & Navigation
- **Lifecycle Runtime Ktx** 2.8.7 — управление жизненным циклом
- **Lifecycle ViewModel Compose** 2.8.7 — ViewModel в Compose
- **Lifecycle Runtime Compose** 2.8.7 — состояния жизненного цикла

### Networking & HTTP
- **OkHttp** 4.10.0 — HTTP-клиент
- **Retrofit** 2.12.0 — REST API клиент
- **Moshi** 1.15.2 — JSON сериализация/десериализация
- **Logging Interceptor** 4.10.0 — логирование HTTP запросов

### Data & Storage
- **Room** 2.7.0 — типобезопасная база данных SQLite
  - Runtime, KTX, Compiler для полной поддержки
- **Shared Preferences** — хранение пользовательских настроек

### Async & Concurrency
- **Coroutines Core** 1.10.2 — асинхронное программирование
- **Coroutines Android** 1.10.2 — интеграция с Android

### KSP & Code Generation
- **Google DevTools KSP** 2.3.5 — генерация кода (Room, Moshi)

### Testing Framework
- **JUnit** 4.13.2 — Unit тестирование
- **Robolectric** 4.16.1 — Android тестирование на JVM
- **Roborazzi** 1.59.0 — Visual snapshot тестирование
- **Espresso** 3.7.0 — UI тестирование
- **AndroidX Test** (Core, Runner, JUnit) — тестовые утилиты

### Build Tools
- **Secrets Gradle Plugin** 2.0.1 — безопасное управление ключами API

**Требования платформы:**
- **Минимальный SDK:** 24 (Android 7.0)  
- **Целевой SDK:** 36 (Android 15)
- **Рекомендуемая версия:** Android 10+ (для лучшей совместимости)

---

<a id="architecture"></a>
## 🏗️ Архитектура

Проект использует **MVVM архитектуру** с четким разделением ответственности и реактивным потоком данных через StateFlow/LiveData.

### Структура пакетов

```
app/src/main/java/com/example/
├── MainActivity.kt                           # Entry point приложения
│
├── ui/                                      # UI Layer (Presentation)
│   ├── BrowserScreen.kt                     # Главное окно браузера (Compose)
│   ├── L10n.kt                              # Локализация (RU, EN, KK)
│   └── theme/                               # Material 3 тема и стили
│
├── viewmodel/                               # ViewModel Layer
│   └── BrowserViewModel.kt                  # Центральный ViewModel со всей бизнес-логикой
│       ├── Web-навигация (URL, история, вкладки)
│       ├── Управление загрузками
│       ├── Фильтрация и сортировка медиа
│       ├── Синхронизация состояния приложения
│       └── Persist настроек в SharedPreferences
│
├── downloader/                              # Service Layer (Загрузки)
│   ├── DownloadManagerHelper.kt             # Менеджер загрузок с поддержкой ZIP
│   │   ├── Загрузка одного файла
│   │   ├── Пакетная загрузка в ZIP
│   │   ├── Проверка размера контента (HEAD/GET)
│   │   └── MediaStore API (Android 10+) и Legacy Storage (Android 9-)
│   │
│   ├── ImageSearchService.kt                # Поиск изображений через Unsplash API
│   │   ├── Динамический поиск с fallback
│   │   └── Кеш предварительных изображений
│   │
│   └── MediaPickerJSInterface.kt            # JavaScript-Kotlin мост для WebView
│
├── model/                                   # Data Layer (Модели)
│   ├── BrowserDatabase.kt                   # Room БД (Bookmarks, History, Tabs, Downloads)
│   ├── BookmarkEntity, HistoryEntity, TabEntity, DownloadEntity
│   ├── MediaItem                            # Структура медиафайла
│   └── SearchImage                          # Результат поиска Unsplash
│
└── AndroidManifest.xml                      # Manifest с перечислением permissions
```

### Примеры использования шаблонов

**MVVM Pattern:**
```kotlin
// UI собирает состояние из ViewModel
val viewModel: BrowserViewModel = viewModel()
val mediaItems by viewModel.filteredAndSortedMedia.collectAsState()
val downloadProgress by viewModel.downloadProgress.collectAsState()

// ViewModel управляет логикой и обновлениями
viewModel.downloadSelectedFilesIndividually()
viewModel.updateFilter("image")
viewModel.toggleBookmark(title, url)
```

**StateFlow для реактивности:**
```kotlin
// Все изменения автоматически распространяются в UI
val filteredAndSortedMedia: StateFlow<List<MediaItem>> = combine(
    _rawMediaItems,
    _filterType,
    _sortOption,
    _mediaSearchQuery
).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

---

<a id="features"></a>
## 🎯 Основной функционал

### 1️⃣ Кастомный поиск и загрузка картинок

**Компонент:** `ImageSearchService.kt`

```kotlin
// Поиск через Unsplash API
suspend fun searchImages(query: String): List<SearchImage>
```

**Особенности:**
- 🔗 Динамический поиск картинок через официальное Unsplash API
- 🚀 **Умный fallback:** если сеть недоступна или API недоступен, возвращает кеш предварительно загруженных тематических изображений
- 🎯 Автоматическое сопоставление тегов и описаний по языку
- 📦 Результаты содержат metadata (photographer name, direct links)
- ⚡ Требует **Gemini API ключ** для расширенной функциональности

**Пример fallback системы:**
- Если Unsplash API не отвечает → используется локальная база из 30+ фотографий
- Фото распределены по категориям: Природа, Животные, Космос, Закаты, Технологии
- Каждая фотография имеет теги и автоматически фильтруется по поисковому запросу

---

### 2️⃣ WebView интеграция и парсинг медиа

**Компоненты:** `BrowserScreen.kt` + `MediaPickerJSInterface.kt`

```kotlin
// JavaScript мост для захвата медиафайлов со страницы
class MediaPickerJSInterface {
    @JavascriptInterface
    fun captureMedia(jsonMediaString: String) {
        // Парсит JSON со всеми обнаруженными img/video/src элементами
        viewModel.parseAndAddDiscoveredMedia(jsonMediaString)
    }
}
```

**Особенности:**
- 📡 **Двусторонний JavaScript-Kotlin мост** для захвата медиа-элементов прямо из DOM
- 🔍 **Парсинг HTML**: проходит по всем `<img>`, `<video>`, `<source>` тегам на странице
- 📊 **Метаданные**: извлекает размеры (width/height), типы файлов, альтернативный текст
- 🧠 **Умная дедупликация**: распознает одно изображение в разных размерах (особенно Pinterest)
  - Автоматически повышает качество для Pinterest путем замены размеров на `/originals/`
  - Группирует изображения по similarity key (нормализованный URL без параметров)
  - Выбирает лучший вариант по качеству (разрешение, размер файла, пути)

```kotlin
// Пример дедупликации для Pinterest
"https://pinimg.com/564x/abc.jpg" + "https://pinimg.com/originals/abc.jpg"
// → Выбирает originals как лучший вариант с повышенным score
```

---

### 3️⃣ Менеджер скачивания медиафайлов

**Компонент:** `DownloadManagerHelper.kt`

#### Функция 1: Загрузка одного файла
```kotlin
suspend fun downloadFile(
    item: MediaItem,
    userAgent: String?,
    referer: String?,
    onProgress: (currentBytes, totalBytes) -> Unit
): Uri?
```

**Особенности:**
- ✅ Поддержка User-Agent и Referer заголовков (имитация браузера)
- 📊 Callback прогресса для от��бражения в UI
- 🔒 Проверка MIME-типов (защита от HTML/XML вместо медиа)
- 💾 Интеллектуальное сохранение:
  - **Android 10+**: MediaStore API (безопасное сохранение с автоматической индексацией)
  - **Android 9-**: Legacy File API (с поддержкой MediaScanner)
- 📁 Автоматическое распределение по папкам:
  - Картинки → `/Pictures/MediaDownloader`
  - Видео → `/Movies/MediaDownloader`
  - Остальное → `/Downloads/MediaDownloader`

#### Функция 2: Проверка размера контента
```kotlin
suspend fun queryContentSize(url: String, userAgent: String?, referer: String?): Long
```

**Особенности:**
- 🔍 Попытка HTTP HEAD запроса
- 📦 Fallback на GET с Range заголовком
- ⚠️ Фильтрует микро-файлы < 1.8 KB (отсеивает пиксельные отслеживания)

#### Функция 3: ZIP архивирование
```kotlin
suspend fun downloadZip(
    items: List<MediaItem>,
    zipFilename: String,
    onItemProgress: (index, total, fraction, name) -> Unit
): Uri?
```

**Особенности:**
- 📦 Пакетная загрузка с сжатием в ZIP
- 📊 Поэлементный callback прогресса
- 🔄 Обработка ошибок отдельных файлов без остановки архива
- 🏷️ Автоматическое санирование имен файлов в архиве

---

<a id="logo-story"></a>
## 🎨 История логотипа (AI-Driven Postmodernism)

> **История, которая могла бы быть рассказана в очень серьёзной конференции по дизайну**

Однажды, окрылённые амбициями, мы обратились к Google AI Studio с простой просьбой:

> _«Создайте красивую 3D-букву "S" со стрелкой, олицетворяющей скорость и плавность браузера.»_

И что же произошло? **Google AI Studio услышал нашу просьбу... но решил её переосмыслить.**

Результат? 🎨 **Два абстрактных колобка, насаженные на зубочистку.**

Или, как минимум, это выглядит так. Возможно, это космонавт, застрявший в текстурах. Или, скорее всего, это просто... два колобка.

**Мы могли бы выбросить это.** Но вместо этого... мы оставили.

Потому что в этой случайности есть совершенство. В этом отказе от контроля есть правда. В этих двух колобках на зубочистке есть дзен.

**✨ Таким образом, "два колобка на зубочистке" стали официальным талисманом Sway Browser — символом того, что иногда лучшие дизайны рождаются из хаоса и принятия неожиданного.**

_Посвящается всем, кто когда-нибудь просил AI что-то красивое и получил нечто абсолютно иное._

---

<a id="installation"></a>
## 📥 Установка

### Требования

| Параметр | Значение |
|----------|----------|
| **Минимальная версия Android** | 7.0 (API 24) |
| **Рекомендуемая версия** | 10.0+ (API 29+) |
| **Свободное место** | ~80-100 МБ |
| **Оперативная память** | 2 ГБ+ (рекомендуется 4 ГБ+) |

### Загрузка

Sway Browser распространяется **бесплатно, напрямую через GitHub Releases**

**👉 [Скачать Latest Release на GitHub Releases](https://raw.githubusercontent.com/RinKBB/Sway-Browser/updates/app-debug.apk)**

Просто загрузите `.apk` файл и установите его на своё устройство.

### Пошаговая установка

1. **Перейдите на страницу [Releases]https://remix-sway-browser-96851841406.asia-southeast1.run.app/)**
2. **Нажмите на кнопку скачивания latest `.apk`**
3. **На устройстве:**
   - Перейдите в **Параметры → Безопасность → Неизвестные источники** (для Android < 8)
   - Или разрешите установку из Chrome/другого приложения (Android 8+)
4. **Откройте загруженный файл** и подтвердите установку
5. **Готово!** 🎉 Приложение будет установлено и готово к использованию

### Требования для установки

- ✅ Разрешение на установку приложений из неизвестных источников
- ✅ Подключение к интернету для загрузки изображений
- ✅ Разрешение на доступ к хранилищу (для сохранения файлов)

---

<a id="development"></a>
## 🔧 Локальная разработка

### Предварительные требования

| Требование | Версия |
|-----------|--------|
| **Android Studio** | Latest (Koala+) |
| **Java Development Kit** | 11+ |
| **Gradle** | 8.x (через wrapper) |
| **Android SDK** | API 24-36 |

### Первый запуск проекта

#### 1. Клонируйте репозиторий
```bash
git clone https://github.com/RinKBB/Sway-Browser.git
cd Sway-Browser
```

#### 2. Откройте в Android Studio
- Файл → Открыть → Выберите директорию проекта
- Android Studio автоматически синхронизирует Gradle

#### 3. Создайте файл конфигурации `.env`
```bash
cp .env.example .env
```

#### 4. Настройте API ключи

**Для функции поиска изображений (Gemini):**
```env
GEMINI_API_KEY=your_gemini_api_key_here
```

Получить API ключ можно здесь: [Google AI Studio](https://aistudio.google.com/app/apikey)

**Для функции поиска изображений (Unsplash):**
```env
UNSPLASH_API_KEY=your_unsplash_api_key_here
```

Получить API ключ можно здесь: [Unsplash Developers](https://unsplash.com/developers)

#### 5. (Опционально) Удалите конфигурацию подписи отладки
Если используете собственный signing config:
- Откройте `app/build.gradle.kts`
- Закомментируйте строку: `signingConfig = signingConfigs.getByName("debugConfig")`

#### 6. Запустите приложение
```bash
# Выберите эмулятор или подключённое устройство
# Нажмите Run (Shift + F10)
```

### Build & Release

**Debug сборка:**
```bash
./gradlew assembleDebug
# APK будет в: app/build/outputs/apk/debug/
```

**Release сборка:**
```bash
./gradlew assembleRelease
# APK будет в: app/build/outputs/apk/release/
```

### Тестирование

```bash
# Unit тесты
./gradlew test

# UI тесты (Roborazzi snapshot)
./gradlew roborazzi

# Все тесты
./gradlew test connectedAndroidTest

# Очистка и пересборка
./gradlew clean build
```

### Отладка

**Включить логирование Retrofit:**
- Уровень логирования уже настроен в `DownloadManagerHelper.kt`
- Используйте Android Studio Logcat для просмотра

**Отладка WebView:**
- Включите отладку веб-контента в параметрах разработчика
- Используйте `chrome://inspect` на компьютере для удаленной отладки

---

## 📚 Ключевые классы

| Класс | Назначение | Примерный размер |
|-------|-----------|-------------------|
| **BrowserViewModel** | Центральная бизнес-логика приложения | ~876 строк |
| **BrowserScreen** | Главная UI Compose функция | ~243 KB |
| **DownloadManagerHelper** | Загрузка и управление файлами | ~323 строк |
| **ImageSearchService** | Поиск изображений Unsplash + Gemini | ~176 строк |
| **BrowserDatabase** | Room БД конфигурация | ~50 строк |
| **MediaPickerJSInterface** | JavaScript-Kotlin мост | ~100 строк |

---

## 🔒 Приватность

- 🛡️ **Блокировка трекеров** — встроенная система фильтрации
- 🗑️ **Очистка при выходе** — опциональное удаление истории
- 🔐 **Режим инкогнито** — отдельные изолированные вкладки
- 📵 **Без аналитики** — приложение не собирает и не отправляет данные пользователей
- 🔒 **Локальное хранилище** — все закладки и история хранятся локально на устройстве

---

## 🌐 Поддерживаемые языки

- 🇷🇺 **Русский** — полная поддержка
- 🇬🇧 **English** — полная поддержка
- 🇰🇿 **Қазақ** — полная поддержка

Новые языки приветствуются! Смотрите `ui/L10n.kt` для добавления переводов.

---

<a id="limitations"></a>
## ⚠️ Известные ограничения

### Функциональные ограничения
- ❌ **Без синхронизации облака** — закладки не синхронизируются между устройствами
- ❌ **Без встроенного VPN** — используйте приложения VPN отдельно
- ❌ **Без расширений браузера** — нет поддержки дополнений
- ❌ **Без встроенного перевода страниц** — используйте встроенный функционал Google Translate

### Проблемы совместимости
- ⚠️ **Некоторые сайты могут отображаться неправильно** на очень старых версиях Android (7.0-8.0)
- ⚠️ **Flash Player не поддерживается** (Flash снят с поддержки)
- ⚠️ **WebRTC может работать нестабильно** на некоторых устройствах

### API и сервисы
- ⚠️ **Unsplash API имеет лимиты** — 50 запросов в час для неавторизованных пользователей
- ⚠️ **Gemini API требует ключ** — без ключа поиск изображений работает в режиме fallback

### Производительность
- ⚠️ **На старых устройствах (RAM < 2GB)** приложение может тормозить при загрузке больших изображений
- ⚠️ **ZIP архивирование** больших наборов файлов может потребовать значительное время

### Известные баги
- 🐛 **Иногда WebView кеш не очищается** полностью при выходе (требует перезагрузки приложения)
- 🐛 **На некоторых Samsung устройствах** может быть проблема с сохранением файлов в MediaStore
- 🐛 **Скачивание очень больших файлов** (> 500 МБ) может привести к ошибкам памяти

---

<a id="support"></a>
## 📞 Поддержка и помощь

### Как сообщить о баге или проблеме

1. **Проверьте [существующие Issues](https://github.com/RinKBB/Sway-Browser/issues)** — возможно, баг уже известен
2. **Создайте новый Issue** с:
   - Описанием проблемы
   - Версией Android
   - Моделью устройства
   - Полным stack trace (если приложение крашит)
   - Шагами для воспроизведения проблемы
3. **Используйте шаблон Issue** для удобства

**👉 [Создать новый Issue](https://github.com/RinKBB/Sway-Browser/issues/new)**

### Как предложить новую функцию

1. **Проверьте [Discussions](https://github.com/RinKBB/Sway-Browser/discussions)** — не обсуждалась ли уже идея
2. **Создайте новую Discussion** в категории "Ideas" с описанием функции
3. **Приложите примеры или макеты**, если это помогает пояснить идею

**👉 [Открыть Discussions](https://github.com/RinKBB/Sway-Browser/discussions)**

### Как внести свой вклад

Мы приветствуем Pull Requests! Для начала:

1. **Fork репозиторий**
2. **Создайте feature branch**: `git checkout -b feature/AmazingFeature`
3. **Commit ваши изменения**: `git commit -m 'Add some AmazingFeature'`
4. **Push в branch**: `git push origin feature/AmazingFeature`
5. **Откройте Pull Request**

Пожалуйста, убедитесь, что:
- ✅ Ваш код следует стилю проекта
- ✅ Все тесты проходят (`./gradlew test`)
- ✅ Вы добавили тесты для новых функций
- ✅ Вы обновили README если нужно

### Связь с автором

- 🐦 **GitHub Issues** — основной способ для вопросов о разработке
- 💬 **Discussions** — для общего обсуждения и идей
- 📧 **Email** — смотрите профиль GitHub (если указан)

---

<a id="license"></a>
## 📝 Лицензия

Этот проект распространяется под лицензией **MIT License**.

Вы можете:
- ✅ Использовать в коммерческих проектах
- ✅ Модифицировать код
- ✅ Распространять
- ✅ Использовать в приватных целях

При условии:
- 📋 Включить копию лицензии
- 📝 Указать автора

Подробнее: [MIT License текст](LICENSE)

---

<div align="center">

## 🎉 Спасибо за внимание!

**Сделано с ❤️ и двумя колобками на зубочистке**

Если вам нравится Sway Browser:
- ⭐ **Поставьте звезду** на GitHub!
- 🔗 **Поделитесь** с друзьями
- 🐛 **Помогите улучшить** — сообщайте о багах
- 💡 **Предлагайте идеи** — открывайте Discussions

### Статус проекта

**🟢 Активная разработка** — приложение постоянно обновляется и улучшается

**Последнее обновление:** 2026 год  
**Версия:** Смотрите [Releases](https://github.com/RinKBB/Sway-Browser/releases)

</div>
