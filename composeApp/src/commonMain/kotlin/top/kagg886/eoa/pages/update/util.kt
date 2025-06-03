package top.kagg886.eoa.pages.update

import kotlinx.serialization.*

@Serializable
data class UpdateInfo(
    val id: Int,
    val tag_name: String,
    val target_commitish: String,
    val prerelease: Boolean,
    val name: String,
    val body: String,
    val author: Author,
    val created_at: String,
    val assets: List<Asset>
)

@Serializable
data class Author(
    val id: Int,
    val login: String,
    val name: String,
    val avatar_url: String,
    val url: String,
    val html_url: String,
    val remark: String,
    val followers_url: String,
    val following_url: String,
    val gists_url: String,
    val starred_url: String,
    val subscriptions_url: String,
    val organizations_url: String,
    val repos_url: String,
    val events_url: String,
    val received_events_url: String,
    val type: String
)

@Serializable
data class Asset(
    val browser_download_url: String,
    val name: String
)