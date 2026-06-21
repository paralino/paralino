package app.paralino.core.utils

import app.paralino.core.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.getImageByFileName

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

    private val userSpecific = listOf(
//        "emoji_u1f4a9",
        "emoji_u1f9a2",
        "emoji_u1f9a5",
        "emoji_u1f9a6",
        "emoji_u1f9a7",
        "emoji_u1f9a9",
        "emoji_u1f40c",
        "emoji_u1f40d",
        "emoji_u1f41d",
        "emoji_u1f41e",
        "emoji_u1f41f",
        "emoji_u1f42c",
        "emoji_u1f42d",
        cow,
        "emoji_u1f42f",
        "emoji_u1f43a",
        "emoji_u1f43b",
        "emoji_u1f43c",
        "emoji_u1f43f",
        "emoji_u1f47b",
        alien,
        "emoji_u1f98a",
        "emoji_u1f98b",
        "emoji_u1f98f",
        "emoji_u1f99c",
        "emoji_u1f383",
        goat,
        "emoji_u1f414",
        "emoji_u1f418",
        "emoji_u1f419",
        "emoji_u1f422",
        "emoji_u1f425",
        "emoji_u1f426",
        "emoji_u1f427",
        "emoji_u1f428",
        "emoji_u1f430",
        "emoji_u1f431",
        "emoji_u1f433",
        "emoji_u1f434",
        "emoji_u1f435",
        "emoji_u1f436",
        "emoji_u1f437",
        "emoji_u1f438",
        "emoji_u1f439",
        "emoji_u1f480",
        "emoji_u1f577",
        robot,
        "emoji_u1f921",
        "emoji_u1f981",
        "emoji_u1f982",
        "emoji_u1f984",
        "emoji_u1f985",
        "emoji_u1f986",
        "emoji_u1f987",
        "emoji_u1f989",
        "emoji_u1f992",
        "emoji_u1f993",
        "emoji_u1f994",
        "emoji_u1f995",
        "emoji_u1f998",
        "emoji_u26c4"
    )

    val groupSpecific = listOf(
        "emoji_u1f3a1",
        "emoji_u1f3a3",
        "emoji_u1f3a4",
        "emoji_u1f3a8",
        "emoji_u1f3aa",
        artMasks,
        "emoji_u1f3ae",
        "emoji_u1f3b3",
        "emoji_u1f3bc",
        "emoji_u1f3be",
        "emoji_u1f3bf",
        basketball,
        "emoji_u1f3c8",
        personGym,
        "emoji_u1f3d0",
        "emoji_u1f3d3",
        "emoji_u1f3d6",
        "emoji_u1f3db",
        scenery,
        "emoji_u1f3df",
        home,
        "emoji_u1f3e2",
        "emoji_u1f3e5",
        "emoji_u1f3eb",
        "emoji_u1f3f0",
        lightBulb,
        moneyBag,
        schoolBooks,
        "emoji_u1f6b2",
        shoppingBags,
        shoppingCart,
        "emoji_u1f6dd",
        suitecase,
        "emoji_u1f30a",
        globe,
        "emoji_u1f31b",
        "emoji_u1f31e",
        "emoji_u1f36d",
        "emoji_u1f37a",
        "emoji_u1f37d",
        "emoji_u1f37f",
        "emoji_u1f93f",
        "emoji_u1f94a",
        "emoji_u1f308",
        "emoji_u1f354",
        "emoji_u1f355",
        "emoji_u1f366",
        "emoji_u1f371",
        "emoji_u1f382",
        "emoji_u1f389",
        "emoji_u1f393",
        "emoji_u1f525",
        "emoji_u1f942",
        croissant,
        "emoji_u1fa82",
        "emoji_u1fab7",
        star,
        "emoji_u26bd",
        "emoji_u26f0",
        "emoji_u26f2",
        "emoji_u26f3",
        "emoji_u26f8",
        "emoji_u26fa",
        "emoji_u2600",
        coffee,
        heart,
    )

    val circles = listOf(
        "emoji_u26aa",
        blackDot,
        "emoji_u1f7e1",
        "emoji_u1f7e2",
        "emoji_u1f534",
        "emoji_u1f535",
    )

    private val transport = listOf(
        "emoji_u1f6f7",
        "emoji_u1f6f9",
        "emoji_u1f682",
        "emoji_u1f697",
        "emoji_u26f4",
        airplane
    )

    val groupEmojis = groupSpecific + circles + transport
    val userEmojis = userSpecific + transport

    fun getResourceFromIcon(icon: String): ImageResource {
        return MR.images.getImageByFileName(icon) ?: MR.images.getImageByFileName(lock)!!
    }
}