package app.paralino.core.utils

import app.paralino.core.resources.moko.*
import dev.icerock.moko.resources.ImageResource

object Emojis {

    val lightBulb = "emoji_u1f4a1"
    val lock = "emoji_u26a0"
    val robot = "emoji_u1f916"
    val star = "emoji_u2b50"
    val cow = "emoji_u1f42e"
    val alien = "emoji_u1f47d"
    val goat = "emoji_u1f410"
    val home = "emoji_u1f3e0"
    val scenery = "emoji_u1f3de"
    val schoolBooks = "emoji_u1f4da"
    val suitecase = "emoji_u1f9f3"
    val heart = "emoji_u2764"
    val coffee = "emoji_u2615"
    val artMasks = "emoji_u1f3ad"
    val airplane = "emoji_u2708"
    val globe = "emoji_u1f30d"
    val mobilePhone = "emoji_u1f4f1"
    val basketball = "emoji_u1f3c0"
    val moneyBag = "emoji_u1f4b0"
    val croissant = "emoji_u1f950"
    val shoppingCart = "emoji_u1f6d2"
    val shoppingBags = "emoji_u1f6cd"
    val personGym = "emoji_u1f3cb"
    val blackDot = "emoji_u26ab"

    private val userSpecificEntries = listOf(
        "emoji_u1f9a2" to MR.images.emoji_u1f9a2,
        "emoji_u1f9a5" to MR.images.emoji_u1f9a5,
        "emoji_u1f9a6" to MR.images.emoji_u1f9a6,
        "emoji_u1f9a7" to MR.images.emoji_u1f9a7,
        "emoji_u1f9a9" to MR.images.emoji_u1f9a9,
        "emoji_u1f40c" to MR.images.emoji_u1f40c,
        "emoji_u1f40d" to MR.images.emoji_u1f40d,
        "emoji_u1f41d" to MR.images.emoji_u1f41d,
        "emoji_u1f41e" to MR.images.emoji_u1f41e,
        "emoji_u1f41f" to MR.images.emoji_u1f41f,
        "emoji_u1f42c" to MR.images.emoji_u1f42c,
        "emoji_u1f42d" to MR.images.emoji_u1f42d,
        cow to MR.images.emoji_u1f42e,
        "emoji_u1f42f" to MR.images.emoji_u1f42f,
        "emoji_u1f43a" to MR.images.emoji_u1f43a,
        "emoji_u1f43b" to MR.images.emoji_u1f43b,
        "emoji_u1f43c" to MR.images.emoji_u1f43c,
        "emoji_u1f43f" to MR.images.emoji_u1f43f,
        "emoji_u1f47b" to MR.images.emoji_u1f47b,
        alien to MR.images.emoji_u1f47d,
        "emoji_u1f98a" to MR.images.emoji_u1f98a,
        "emoji_u1f98b" to MR.images.emoji_u1f98b,
        "emoji_u1f98f" to MR.images.emoji_u1f98f,
        "emoji_u1f99c" to MR.images.emoji_u1f99c,
        "emoji_u1f383" to MR.images.emoji_u1f383,
        goat to MR.images.emoji_u1f410,
        "emoji_u1f414" to MR.images.emoji_u1f414,
        "emoji_u1f418" to MR.images.emoji_u1f418,
        "emoji_u1f419" to MR.images.emoji_u1f419,
        "emoji_u1f422" to MR.images.emoji_u1f422,
        "emoji_u1f425" to MR.images.emoji_u1f425,
        "emoji_u1f426" to MR.images.emoji_u1f426,
        "emoji_u1f427" to MR.images.emoji_u1f427,
        "emoji_u1f428" to MR.images.emoji_u1f428,
        "emoji_u1f430" to MR.images.emoji_u1f430,
        "emoji_u1f431" to MR.images.emoji_u1f431,
        "emoji_u1f433" to MR.images.emoji_u1f433,
        "emoji_u1f434" to MR.images.emoji_u1f434,
        "emoji_u1f435" to MR.images.emoji_u1f435,
        "emoji_u1f436" to MR.images.emoji_u1f436,
        "emoji_u1f437" to MR.images.emoji_u1f437,
        "emoji_u1f438" to MR.images.emoji_u1f438,
        "emoji_u1f439" to MR.images.emoji_u1f439,
        "emoji_u1f480" to MR.images.emoji_u1f480,
        "emoji_u1f577" to MR.images.emoji_u1f577,
        robot to MR.images.emoji_u1f916,
        "emoji_u1f921" to MR.images.emoji_u1f921,
        "emoji_u1f981" to MR.images.emoji_u1f981,
        "emoji_u1f982" to MR.images.emoji_u1f982,
        "emoji_u1f984" to MR.images.emoji_u1f984,
        "emoji_u1f985" to MR.images.emoji_u1f985,
        "emoji_u1f986" to MR.images.emoji_u1f986,
        "emoji_u1f987" to MR.images.emoji_u1f987,
        "emoji_u1f989" to MR.images.emoji_u1f989,
        "emoji_u1f992" to MR.images.emoji_u1f992,
        "emoji_u1f993" to MR.images.emoji_u1f993,
        "emoji_u1f994" to MR.images.emoji_u1f994,
        "emoji_u1f995" to MR.images.emoji_u1f995,
        "emoji_u1f998" to MR.images.emoji_u1f998,
        "emoji_u26c4" to MR.images.emoji_u26c4
    )

    private val groupSpecificEntries = listOf(
        "emoji_u1f3a1" to MR.images.emoji_u1f3a1,
        "emoji_u1f3a3" to MR.images.emoji_u1f3a3,
        "emoji_u1f3a4" to MR.images.emoji_u1f3a4,
        "emoji_u1f3a8" to MR.images.emoji_u1f3a8,
        "emoji_u1f3aa" to MR.images.emoji_u1f3aa,
        artMasks to MR.images.emoji_u1f3ad,
        "emoji_u1f3ae" to MR.images.emoji_u1f3ae,
        "emoji_u1f3b3" to MR.images.emoji_u1f3b3,
        "emoji_u1f3bc" to MR.images.emoji_u1f3bc,
        "emoji_u1f3be" to MR.images.emoji_u1f3be,
        "emoji_u1f3bf" to MR.images.emoji_u1f3bf,
        basketball to MR.images.emoji_u1f3c0,
        "emoji_u1f3c8" to MR.images.emoji_u1f3c8,
        personGym to MR.images.emoji_u1f3cb,
        "emoji_u1f3d0" to MR.images.emoji_u1f3d0,
        "emoji_u1f3d3" to MR.images.emoji_u1f3d3,
        "emoji_u1f3d6" to MR.images.emoji_u1f3d6,
        "emoji_u1f3db" to MR.images.emoji_u1f3db,
        scenery to MR.images.emoji_u1f3de,
        "emoji_u1f3df" to MR.images.emoji_u1f3df,
        home to MR.images.emoji_u1f3e0,
        "emoji_u1f3e2" to MR.images.emoji_u1f3e2,
        "emoji_u1f3e5" to MR.images.emoji_u1f3e5,
        "emoji_u1f3eb" to MR.images.emoji_u1f3eb,
        "emoji_u1f3f0" to MR.images.emoji_u1f3f0,
        lightBulb to MR.images.emoji_u1f4a1,
        moneyBag to MR.images.emoji_u1f4b0,
        schoolBooks to MR.images.emoji_u1f4da,
        "emoji_u1f6b2" to MR.images.emoji_u1f6b2,
        shoppingBags to MR.images.emoji_u1f6cd,
        shoppingCart to MR.images.emoji_u1f6d2,
        "emoji_u1f6dd" to MR.images.emoji_u1f6dd,
        suitecase to MR.images.emoji_u1f9f3,
        "emoji_u1f30a" to MR.images.emoji_u1f30a,
        globe to MR.images.emoji_u1f30d,
        "emoji_u1f31b" to MR.images.emoji_u1f31b,
        "emoji_u1f31e" to MR.images.emoji_u1f31e,
        "emoji_u1f36d" to MR.images.emoji_u1f36d,
        "emoji_u1f37a" to MR.images.emoji_u1f37a,
        "emoji_u1f37d" to MR.images.emoji_u1f37d,
        "emoji_u1f37f" to MR.images.emoji_u1f37f,
        "emoji_u1f93f" to MR.images.emoji_u1f93f,
        "emoji_u1f94a" to MR.images.emoji_u1f94a,
        "emoji_u1f308" to MR.images.emoji_u1f308,
        "emoji_u1f354" to MR.images.emoji_u1f354,
        "emoji_u1f355" to MR.images.emoji_u1f355,
        "emoji_u1f366" to MR.images.emoji_u1f366,
        "emoji_u1f371" to MR.images.emoji_u1f371,
        "emoji_u1f382" to MR.images.emoji_u1f382,
        "emoji_u1f389" to MR.images.emoji_u1f389,
        "emoji_u1f393" to MR.images.emoji_u1f393,
        "emoji_u1f525" to MR.images.emoji_u1f525,
        "emoji_u1f942" to MR.images.emoji_u1f942,
        croissant to MR.images.emoji_u1f950,
        "emoji_u1fa82" to MR.images.emoji_u1fa82,
        "emoji_u1fab7" to MR.images.emoji_u1fab7,
        star to MR.images.emoji_u2b50,
        "emoji_u26bd" to MR.images.emoji_u26bd,
        "emoji_u26f0" to MR.images.emoji_u26f0,
        "emoji_u26f2" to MR.images.emoji_u26f2,
        "emoji_u26f3" to MR.images.emoji_u26f3,
        "emoji_u26f8" to MR.images.emoji_u26f8,
        "emoji_u26fa" to MR.images.emoji_u26fa,
        "emoji_u2600" to MR.images.emoji_u2600,
        coffee to MR.images.emoji_u2615,
        heart to MR.images.emoji_u2764,
    )

    private val circleEntries = listOf(
        "emoji_u26aa" to MR.images.emoji_u26aa,
        blackDot to MR.images.emoji_u26ab,
        "emoji_u1f7e1" to MR.images.emoji_u1f7e1,
        "emoji_u1f7e2" to MR.images.emoji_u1f7e2,
        "emoji_u1f534" to MR.images.emoji_u1f534,
        "emoji_u1f535" to MR.images.emoji_u1f535,
    )

    private val transportEntries = listOf(
        "emoji_u1f6f7" to MR.images.emoji_u1f6f7,
        "emoji_u1f6f9" to MR.images.emoji_u1f6f9,
        "emoji_u1f682" to MR.images.emoji_u1f682,
        "emoji_u1f697" to MR.images.emoji_u1f697,
        "emoji_u26f4" to MR.images.emoji_u26f4,
        airplane to MR.images.emoji_u2708
    )

    // Not in pickers, but used directly or by previously chosen icons
    private val otherEntries = listOf(
        lock to MR.images.emoji_u26a0,
        mobilePhone to MR.images.emoji_u1f4f1,
        "emoji_u1f4a9" to MR.images.emoji_u1f4a9,
    )

    val groupSpecific = groupSpecificEntries.map { it.first }
    val circles = circleEntries.map { it.first }

    val groupEmojis = (groupSpecificEntries + circleEntries + transportEntries).map { it.first }
    val userEmojis = (userSpecificEntries + transportEntries).map { it.first }

    private val resources: Map<String, ImageResource> = (
        userSpecificEntries + groupSpecificEntries + circleEntries + transportEntries + otherEntries
    ).toMap()

    fun getResourceFromIcon(icon: String): ImageResource {
        return resources[icon] ?: resources.getValue(lock)
    }
}
