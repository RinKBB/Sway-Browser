<div align="center">

# 🌊 Sway Browser

**Современный Android-браузер с интеллектуальным загрузчиком медиафайлов**

[📥 Скачать APK](#установка) • [🏗️ Архитектура](#архитектура) • [🛠️ Стек технологий](#технический-стек) • [🎯 Функционал](#основной-функционал)

</div>

---

## 📋 Содержание

1. [О проекте](#о-проекте)
2. [Технический стек](#технический-стек)
3. [Архитектура](#архитектура)
4. [Основной функционал](#основной-функционал)
5. [История логотипа](#история-логотипа-ai-driven-postmodernism)
6. [Установка](#установка)
7. [Локальная разработка](#локальная-разработка)
8. [Лицензия](#лицензия)

---

## 📱 О проекте

**Sway Browser** — это полнофункциональный мобильный браузер для Android, разработанный на современном стеке Jetpack Compose и Material 3. Приложение отличается:

- ✨ **Элегантным интерфейсом** на Material Design 3
- 🎯 **Интеллектуальной системой обнаружения** медиафайлов на веб-страницах
- 📥 **Мощным менеджером загрузок** с поддержкой ZIP-архивов
- 🌍 **Кастомным поиском изображений** через Unsplash API
- 🔒 **Встроенными функциями приватности** (блокировка трекеров, очистка истории)
- 🎨 **Поддержкой нескольких языков** (Русский, Английский, Казахский)

---

## 🛠️ Технический стек

### Core Framework & Build
- **Kotlin** 2.2.10 — язык разработки
- **Android Gradle Plugin** 9.1.1 — система сборки
- **Java** 11 — целевая версия JVM

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
- **Gradle** 8.x — система сборки (через wrapper)

**Минимальный SDK:** 24 (Android 7.0)  
**Целевой SDK:** 36 (Android 15)

---

## 🏗️ Архитектура

Проект использует **MVVM архитектуру** с четким разделением ответственности и реактивным потоком данных через Kotlin Coroutines и StateFlow.

### Структура пакетов

```
app/src/main/java/com/example/
├── MainActivity.kt                           # Entry point приложения
│
├── ui/                                      # UI Layer (Presentation)
│   ├── BrowserScreen.kt                     # Главное окно браузера (244 KB Compose UI)
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

## 🎯 Основной функционал

### 1️⃣ Кастомный поиск и загрузка картинок

**Компонент:** `ImageSearchService.kt`

```kotlin
// Поиск через Unsplash NAPI
suspend fun searchImages(query: String): List<SearchImage>
```

**Особенности:**
- 🔗 Динамический поиск картинок через официальное Unsplash API
- 🚀 Умный fallback: если сеть недоступна, вернет кеш предварительно з��груженных тематических изображений
- 🎯 Автоматическое сопоставление тегов и описаний по языку
- 📦 Результаты содержат metadata (photographer name, direct links)

**Пример fallback системы:**
- Если Unsplash NAPI не отвечает → используется локальная база из 30+ фотографий
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
- 📡 **��вусторонний JavaScript-Kotlin мост** для захвата медиа-элементов прямо из DOM
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

#### Функция 1: Загру��ка одного файла
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
- 📊 Callback прогресса для отображения в UI
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
- ⚠️ Фильтрует микросм файлы < 1.8 KB (отсеивает пиксели отслеживания)

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

## 🎨 История логотипа (AI-Driven Postmodernism)

> **История, которая могла бы быть рассказана в очень серьёзной конференции по дизайну**

Однажды, окрылённые амбициями, мы обратились к Google AI Studio с простой просьбой:

> _«Создайте красивую 3D-букву "S" со стрелкой, олицетворяющей скорость и плавность браузера.»_

И что же произошло? **Google AI Studio услышал нашу просьбу... но решил её переосмыслить.**

Результат? 🎨 **Два абстрактных колобка, насаженные на зубочистку.**

Или, как минимум, это выглядит так. Возможно, это космонавт, застрявший в текстурах. Или, скорее всего, это поэтическое воплощение постмодернизма через призму нейросетей — две сущности в вечном танце через пространство-время, соединённые зубочисткой бытия.

**Мы могли бы выбросить это.** Но вместо этого... мы оставили.

Потому что в этой случайности есть совершенство. В этом отказе от контроля есть правда. В этих двух колобках и их зубочистке — вся суть нашей эпохи.

**✨ Таким образом, "два колобка на зубочистке" стали официальным талисманом Sway Browser — символом того, что иногда лучшие идеи рождаются совершенно неожиданно.**

_Посвящается всем, кто когда-нибудь просил AI что-то красивое и получил нечто абсолютно иное._

---

## 📥 Установка

### Требования
- 📱 **Android 7.0+** (API 24)
- 💾 ~80 МБ свободного места

### Загрузка

Мы распространяем Sway Browser **бесплатно, напрямую через GitHub Pages**, избегая комиссии Google Play ($25 за приложение). 

**👉 [Скачать Latest Release на GitHub Pages](https://rinkbb.github.io/sway-browser/)**

Просто загрузите `.apk` файл и установите его на своё устройство.

### Установка вручную

1. Посетите: https://rinkbb.github.io/sway-browser/
2. Нажмите на кнопку скачивания latest `.apk`
3. На устройстве перейдите в **Параметры → Безопасность → Неизвестные источники** (для Android < 8)
4. Откройте загруженный файл и подтвердите установку
5. Готово! 🎉

---

## 🔧 Локальная разработка

### Пред��арительные требования
- 🔽 [Android Studio](https://developer.android.com/studio) (Latest)
- ☕ Java 11+
- 📦 Gradle 8.x (через wrapper)

### Первый запуск

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/RinKBB/Sway-Browser.git
   cd Sway-Browser
   ```

2. **Откройте в Android Studio:**
   - Файл → Открыть → Выберите директорию проекта
   - Android Studio автоматически синхронизирует Gradle

3. **Настройте переменные окружения:**
   - Скопируйте `.env.example` в `.env`:
     ```bash
     cp .env.example .env
     ```
   - Отредактируйте `.env` и укажите ваш **Gemini API key**:
     ```env
     GEMINI_API_KEY=your_api_key_here
     ```

4. **Удалите конфигурацию подписи отладки** (если нужно):
   - Откройте `app/build.gradle.kts`
   - Закомментируйте строку: `signingConfig = signingConfigs.getByName("debugConfig")`

5. **Запустите приложение:**
   - Выберите эмулятор или подключённое устройство
   - Нажмите **Run** (Shift + F10)

### Build & Release

**Debug сборка:**
```bash
./gradlew assembleDebug
```

**Release сборка:**
```bash
./gradlew assembleRelease
```

### Тестирование

```bash
# Unit тесты
./gradlew test

# UI тесты (Roborazzi snapshot)
./gradlew roborazzi

# Все тесты
./gradlew test connectedAndroidTest
```

---

## 📚 Ключевые классы

| Класс | Назначение | Строк |
|-------|-----------|-------|
| **BrowserViewModel** | Центральная бизнес-логика приложения | ~876 |
| **BrowserScreen** | Главная UI Compose функция | ~243 KB |
| **DownloadManagerHelper** | Загрузка и управление файлами | ~323 |
| **ImageSearchService** | Поиск изображений Unsplash | ~176 |
| **BrowserDatabase** | Room БД конфигурация | - |

---

## 🔒 Приватность

- 🛡️ **Блокировка трекеров** — встроенная система фильтрации
- 🗑️ **Очистка при выходе** — опциональное удаление истории
- 🔐 **Режим инкогнито** — отдельные изолированные вкладки
- 📵 **Без аналитики** — приложение не собирает данные

---

## 🌐 Поддерживаемые языки

- 🇷🇺 **Русский**
- 🇬🇧 **English**
- 🇰🇿 **Қазақ**

---

## 📝 Лицензия

Этот проект распространяется под лицензией **MIT License**.

---

<div align="center">

**Сделано с ❤️ и двумя колобками на зубочистке**

Если вам нравится Sway Browser, ⭐ **поставьте звезду** на GitHub!

</div>
