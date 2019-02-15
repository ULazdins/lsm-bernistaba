package lv.ugis.lsmbernistaba

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.Serializable

data class AudioCategory (
    var url: String,
    var title: String
): Serializable {
    companion object {
        internal const val serialVersionUID = 727566175075960653L
    }
}

data class AudioItem (
    var url: String,
    var imageUrl: String,
    var title: String
): Serializable {
    companion object {
        internal const val serialVersionUID = 727566175075960653L
    }
}

class ApiClient {
    fun loadAudioCategories(): Single<List<AudioCategory>> {
        val o: Single<List<AudioCategory>> = Single.create {
            try {
                // Connect to the web site
                val mBlogDocument = Jsoup.connect("https://bernistaba.lsm.lv/klausies").get()
                // Using Elements to get the Meta data
                val mElementDataSize: Elements = mBlogDocument.select("div[class=swiper-slide audio]")
                val items: List<AudioCategory> = mElementDataSize.map {
                    val url: String = it.select("a[class=item]").attr("href")
                    val title: String = it
                        .select("div[class=item-title]")
                        .text()

                    AudioCategory(url, title)
                }
                it.onSuccess(items)
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        return o.subscribeOn(Schedulers.io())
    }

    fun loadAudioItemsForCategory(uri: String): Single<List<AudioItem>> {
        val o: Single<List<AudioItem>> = Single.create {
            try {
                // Connect to the web site
                val mBlogDocument = Jsoup.connect(uri).get()
                // Using Elements to get the Meta data
                val mElementDataSize: Elements = mBlogDocument.select("a[class=audio-item]")
                val items: List<AudioItem> = mElementDataSize.map {
                    val url: String = it.attr("href")
                    val imageUrl: String = it
                        .select("div[class=item-thumb]")
                        .attr("style")
                        .removePrefix("background-image:url(")
                        .removeSuffix(")")
                    val title: String = it
                        .select("div[class=item-title]")
                        .text()

                    AudioItem(url, imageUrl, title)
                }
                it.onSuccess(items)
            } catch (e: Exception) {
                it.onError(e)
            }
        }

        return o.subscribeOn(Schedulers.io())
    }
}