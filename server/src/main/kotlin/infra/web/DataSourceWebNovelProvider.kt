package infra.web

import infra.model.Page
import infra.web.providers.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class DataSourceWebNovelProvider(
    httpsProxy: String?,
    pixivPhpsessid: String?,
) {
    private val client = HttpClient(Java) {
        install(HttpCookies) {
            default {
                Alphapolis.addCookies(this)
                Hameln.addCookies(this)
                Pixiv.addCookies(this, phpsessid = pixivPhpsessid)
                Syosetu.addCookies(this)
            }
        }
        install(ContentNegotiation) {
            json(Json { isLenient = true })
        }
        expectSuccess = true
        httpsProxy?.let {
            engine {
                proxy = ProxyBuilder.http(it)
            }
        }
    }

    private val providers = mapOf(
        Alphapolis.id to Alphapolis(client),
        Hameln.id to Hameln(client),
        Kakuyomu.id to Kakuyomu(client),
        Novelup.id to Novelup(client),
        Pixiv.id to Pixiv(client),
        Syosetu.id to Syosetu(client),
    )

    suspend fun listRank(providerId: String, options: Map<String, String>): Result<Page<RemoteNovelListItem>> {
        return runCatching {
            providers[providerId]!!.getRank(options)
        }
    }

    suspend fun getMetadata(providerId: String, novelId: String): Result<RemoteNovelMetadata> {
        return runCatching {
            providers[providerId]!!.getMetadata(novelId)
        }
    }

    suspend fun getChapter(providerId: String, novelId: String, chapterId: String): Result<RemoteChapter> {
        return runCatching {
            providers[providerId]!!.getChapter(novelId, chapterId)
        }
    }
}