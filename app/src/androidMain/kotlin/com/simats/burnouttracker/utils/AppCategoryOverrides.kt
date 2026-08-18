package com.simats.burnouttracker.utils

/**
 * Deterministic package-name -> category overrides, checked before the
 * on-device ML classifier in [AppUsageClassifier].
 *
 * The ML model's vocabulary is missing common app names outright (e.g.
 * "spotify" has no vocab entry at all, so its input vector is all zeros) and
 * even in-vocabulary names ("hotstar") aren't enough signal to beat the
 * model's default class. Keyed on package name rather than display name
 * since package names are stable across locale and OEM rebranding.
 */
object AppCategoryOverrides {
    private val streaming = listOf(
        "spotify", "netflix", "hotstar", "disneyplus", "disney", "primevideo",
        "amazon.avod", "hulu", "hbomax", "hbo", "youtube", "jiocinema", "voot",
        "zee5", "sonyliv", "crunchyroll", "twitch.android", "soundcloud",
        "gaana", "wynk", "apple.android.music", "pandora", "tidal", "deezer",
        "google.android.videos", "plex", "tubi", "audible"
    )
    private val social = listOf(
        "instagram", "facebook.katana", "facebook.orca", "facebook.lite",
        "twitter", "snapchat", "whatsapp", "telegram", "linkedin", "pinterest",
        "reddit", "discord", "zhiliaoapp.musically", "ss.android.ugc.trill",
        "tumblr", "instagram.barcelona", "vk.com", "weibo"
    )
    private val gaming = listOf(
        "tencent.ig", "pubg", "dts.freefireth", "mojang", "roblox",
        "supercell", "mihoyo", "king.candycrushsaga", "activision.callofduty",
        "ea.gp.fifamobile", "gameloft", "innersloth", "garena",
        "nianticlabs.pokemongo", "epicgames.fortnite", "riotgames"
    )
    private val productivity = listOf(
        "microsoft.office", "microsoft.teams", "microsoft.outlook", "slack",
        "google.android.apps.docs", "google.android.gm", "google.android.calendar",
        "google.android.keep", "notion.id", "todoist", "asana", "trello",
        "dropbox", "evernote", "zoom.videomeetings", "google.android.apps.meetings",
        "adobe.reader", "adobe.acrobat", "canva"
    )

    // Preinstalled utility apps that are real, deliberate opens (so
    // isSystemPackage lets them through) but don't semantically belong in
    // any of the four burnout-relevant buckets. Routed here explicitly so
    // they land on "Others" instead of being handed to the ML model, which
    // has been observed guessing e.g. Maps into "Social Media".
    //
    // General-purpose browsers are here too, not in one of the four buckets:
    // "browsing" spans social feeds, video, shopping and work equally, so
    // there's no single bucket that's more right than the others, and both
    // the OS-declared category and the ML model have been observed guessing
    // one anyway (a browser mis-landing in Social Media inflates that bucket
    // by hours it has no real claim to).
    private val utility = listOf(
        "apps.maps", "google.android.dialer", "android.dialer",
        "google.android.calculator", "android.calculator2",
        "google.android.gm.calculator", "android.camera", "google.android.gallery3d",
        "google.android.apps.photos", "filemanager", "myfiles", "documentsui",
        "fileexplorer", "google.android.deskclock", "android.deskclock",
        "android.chrome", "chrome.beta", "chrome.dev", "chrome.canary",
        "mozilla.firefox", "opera.browser", "opera.mini", "microsoft.emmx",
        "brave.browser", "duckduckgo.mobile", "sec.android.app.sbrowser",
        "ucmobile", "kiwibrowser", "mi.globalbrowser"
    )

    fun match(packageName: String): String? {
        val low = packageName.lowercase()
        return when {
            streaming.any { low.contains(it) } -> "Streaming"
            social.any { low.contains(it) } -> "Social Media"
            gaming.any { low.contains(it) } -> "Gaming"
            productivity.any { low.contains(it) } -> "Productivity"
            utility.any { low.contains(it) } -> "Others"
            else -> null
        }
    }
}
