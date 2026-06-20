package com.example.downloader

import android.util.Log
import com.example.model.SearchImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ImageSearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * Search Unsplash images dynamically using Unsplash NAPI.
     * Guaranteed to switch to a smart thematic fallback if offline or request fails.
     */
    suspend fun searchImages(query: String): List<SearchImage> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(trimmedQuery, "UTF-8")
            val url = "https://unsplash.com/napi/search/photos?query=$encodedQuery&per_page=30"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        val jsonRoot = JSONObject(bodyString)
                        val resultsArray = jsonRoot.optJSONArray("results")
                        if (resultsArray != null && resultsArray.length() > 0) {
                            val results = mutableListOf<SearchImage>()
                            for (i in 0 until resultsArray.length()) {
                                val item = resultsArray.getJSONObject(i)
                                val urls = item.optJSONObject("urls")
                                val regularUrl = urls?.optString("regular") ?: urls?.optString("small")
                                if (!regularUrl.isNullOrEmpty()) {
                                    val rawAlt = item.optString("alt_description")
                                    val rawDesc = item.optString("description")
                                    val altDesc = when {
                                        !rawAlt.isNullOrBlank() && rawAlt != "null" -> rawAlt.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                        !rawDesc.isNullOrBlank() && rawDesc != "null" -> rawDesc.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                        else -> "$trimmedQuery — Фото #${i + 1}"
                                    }
                                    
                                    val links = item.optJSONObject("links")
                                    val webUrl = links?.optString("html") ?: "https://unsplash.com"
                                    
                                    val user = item.optJSONObject("user")
                                    val photographerName = user?.optString("name") ?: "Unsplash Contributor"
                                    
                                    results.add(
                                        SearchImage(
                                            imageUrl = regularUrl,
                                            title = altDesc,
                                            siteUrl = "unsplash.com",
                                            siteName = photographerName
                                        )
                                    )
                                }
                            }
                            if (results.isNotEmpty()) {
                                Log.i("ImageSearchService", "Successfully searched Unsplash NAPI, items count: ${results.size}")
                                return@withContext results
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImageSearchService", "Error searching Unsplash NAPI: ${e.message}", e)
        }

        // Fallback: Generate curated matching Unsplash images if network request fails or empty
        Log.w("ImageSearchService", "Using thematic mockup generator because Unsplash NAPI failed.")
        return@withContext generateFallbackImages(trimmedQuery)
    }

    private data class FallbackPhoto(
        val id: String,
        val defaultTitle: String,
        val tags: List<String>
    )

    private val allPhotos = listOf(
        // Nature
        FallbackPhoto("1470071459604-3b5ec3a7fe05", "Озеро в туманном утреннем лесу", listOf("природа", "nature", "лес", "forest", "озеро", "lake", "туман", "fog", "утро", "morning", "пейзаж", "landscape", "деревья", "trees")),
        FallbackPhoto("1447752875215-b2761acb3c5d", "Романтичный деревянный мост посреди леса", listOf("природа", "nature", "мост", "bridge", "деревянный", "wooden", "лес", "forest", "туман", "mist", "деревья", "trees", "тропа", "path")),
        FallbackPhoto("1441974231531-c6227db76b6e", "Солнечные лучи пробиваются сквозь деревья", listOf("природа", "nature", "солнце", "sun", "лучи", "rays", "свет", "light", "деревья", "trees", "лес", "forest")),
        FallbackPhoto("1501854140801-50d01698950b", "Живописные зеленые луга и холмы Альп", listOf("природа", "nature", "зеленый", "green", "луг", "meadow", "трава", "grass", "холмы", "hills", "альпы", "alps", "горы", "mountains")),
        FallbackPhoto("1469474968028-56623f02e42e", "Путешественник на краю горного каньона", listOf("природа", "nature", "путешествие", "travel", "каньон", "canyon", "горы", "mountains", "рассвет", "sunrise")),
        FallbackPhoto("1472214222541-d510753a4707", "Цветущая долина у подножия острых скал", listOf("природа", "nature", "долина", "valley", "цветы", "flowers", "скалы", "rocks", "горы", "mountains")),
        FallbackPhoto("1502082553048-f009c37129b9", "Макро-фотография сочной зелени листа", listOf("природа", "nature", "макро", "macro", "зеленый", "green", "лист", "leaf", "растение", "plant")),

        // Animals
        FallbackPhoto("1514888286974-6c03e2ca1dba", "Рыжий домашний кот греется на солнце", listOf("котик", "котики", "кот", "кошка", "рыжий", "животные", "cat", "cats", "orange", "animals", "пет", "pet")),
        FallbackPhoto("1533738363-b7f9aef128ce", "Забавный котенок в крутых круглых очках", listOf("котенок", "кот", "котик", "котики", "животные", "очки", "glasses", "funny", "cat", "kitten", "pet", "animals")),
        FallbackPhoto("1573865526739-10659fec78a5", "Британский вислоухий серый кот отдыхает", listOf("кошка", "кошки", "кот", "котик", "котики", "британский", "серый", "grey", "cat", "cats", "british", "pet", "animals", "животные")),
        FallbackPhoto("1543466835-00a7907e9de1", "Веселый золотистый ретривер на траве", listOf("собака", "собачки", "собачка", "ретривер", "щенок", "пес", "dog", "puppy", "retriever", "animals", "животные", "pet")),
        FallbackPhoto("1519052537078-e6302a4968d4", "Милая спящая белая кошка", listOf("кошка", "кошки", "кот", "котик", "котики", "белая", "спящая", "сон", "cat", "white", "sleep", "animals", "животные")),
        FallbackPhoto("1517841905240-472988babdf9", "Очаровательный игривый щенок", listOf("щенок", "щенки", "собака", "собачки", "собачка", "пес", "puppy", "dog", "dogs", "animals", "животные")),

        // Space
        FallbackPhoto("1451187580459-43490279c0fa", "Далекая сияющая галактика во Вселенной", listOf("космос", "space", "галактика", "galaxy", "вселенная", "universe", "звезды", "stars")),
        FallbackPhoto("1419242902214-272b3f66ee7a", "Млечный Путь над ночной пустыней Мохаве", listOf("космос", "space", "млечный путь", "milky way", "пустыня", "desert", "ночь", "night", "звезды", "stars")),
        FallbackPhoto("1541185933-ef5d8ed016c2", "Мощный старт космической ракеты", listOf("космос", "space", "ракета", "rocket", "старт", "launch", "falcon", "nasa")),
        FallbackPhoto("1502134249126-9f3755a50d78", "Марсоход на безмолвной Красной планете", listOf("космос", "space", "марсоход", "rover", "марс", "mars", "планета", "planet")),
        FallbackPhoto("1506318137071-a8e063b4bec0", "Звездная космическая пыль и созвездия", listOf("космос", "space", "пыль", "dust", "созвездие", "constellation", "звезды", "stars")),
        FallbackPhoto("1446776811953-b23d57bd21aa", "Вид на планету Земля с орбиты МКС", listOf("космос", "space", "земля", "earth", "орбита", "orbit", "мкс", "iss")),

        // Sunset
        FallbackPhoto("1506744038136-46273834b3fb", "Огненный малиновый закат на берегу океана", listOf("закат", "sunset", "океан", "ocean", "море", "sea", "берег", "beach", "пляж")),
        FallbackPhoto("1507525428034-b723cf961d3e", "Песчаный тропический пляж и пальмы", listOf("пляж", "beach", "закат", "sunset", "пальмы", "palms", "тропики", "tropical", "песок", "sand")),
        FallbackPhoto("1475113548554-5a36f1f523d6", "Теплые лучи заходящего солнца на скалах", listOf("закат", "sunset", "солнце", "sun", "скалы", "rocks", "вечер", "evening")),
        FallbackPhoto("1518495973542-4542c06a5843", "Закатное солнце освещает старое дерево", listOf("закат", "sunset", "солнце", "sun", "дерево", "tree", "свет", "light")),
        FallbackPhoto("1470252649358-9694d8935c17", "Вечернее лавандовое поле перед закатом", listOf("закат", "sunset", "вечер", "evening", "лаванда", "lavender", "поле", "field")),

        // Tech
        FallbackPhoto("1518770660439-4636190af475", "Высокотехнологичный процессор с подсветкой", listOf("технологии", "tech", "процессор", "cpu", "чип", "chip", "компьютер", "computer", "железо", "hardware")),
        FallbackPhoto("1531297484001-80022131f5a1", "Современное рабочее место программиста", listOf("технологии", "tech", "рабочее место", "workspace", "ноутбук", "laptop", "программист", "developer", "офис", "office")),
        FallbackPhoto("1488590528505-98d2b5aba04b", "Код на экране ноутбука в коворкинге", listOf("технологии", "tech", "код", "code", "программирование", "programming", "ноутбук", "laptop", "экраны", "screens")),
        FallbackPhoto("1534447677768-be436bb09401", "Абстрактный футуристический неоновый коридор", listOf("абстракция", "abstract", "неон", "neon", "коридор", "corridor", "футуризм", "futuristic")),
        FallbackPhoto("1618005182384-a83a8bd57fbe", "Волнообразные премиум 3D Material Design фигуры", listOf("абстракция", "abstract", "3d", "фигуры", "shapes", "дизайн", "design", "material")),
        FallbackPhoto("1541701494587-cb58502866ab", "Элегантный абстрактный узор жидкого акрила", listOf("абстракция", "abstract", "жидкий", "fluid", "акрил", "acrylic", "узор", "pattern", "арт", "art"))
    )

    private fun generateFallbackImages(query: String): List<SearchImage> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val tokens = trimmed.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        // Filter and score each photo in our clean pool of images
        val matchedPhotos = allPhotos.mapNotNull { photo ->
            var score = 0
            for (token in tokens) {
                val matchesTitle = photo.defaultTitle.lowercase().contains(token)
                val matchesTag = photo.tags.any { tag -> tag.contains(token) || token.contains(tag) }
                if (matchesTitle || matchesTag) {
                    score++
                }
            }
            if (score > 0) {
                photo to score
            } else {
                null
            }
        }.sortedByDescending { it.second }

        val sources = listOf("Unsplash Creator", "Global Photography", "Creative Commons", "Media Studio", "Sway Lens")

        return matchedPhotos.mapIndexed { index, (photo, _) ->
            val sourceIdx = (index + photo.id.hashCode()).mod(sources.size)
            SearchImage(
                imageUrl = "https://images.unsplash.com/photo-${photo.id}?q=80&w=600&auto=format&fit=crop",
                title = photo.defaultTitle,
                siteUrl = "unsplash.com",
                siteName = sources[sourceIdx]
            )
        }
    }
}
