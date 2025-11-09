package org.inquest.discord

import discord4j.discordjson.json.EmojiData
import discord4j.rest.util.Color

object CustomEmojis {
    val TIME_EMOJI: EmojiData = EmojiData.builder().id(1340019219638652968L).name("time").build()
    val GROUP_DPS_EMOJI: EmojiData = EmojiData.builder().id(1340055121337389217).name("groupdps").build()
    val DPS_EMOJI: EmojiData = EmojiData.builder().id(1340084146755735583L).name("dps").build()
    val INFO_EMOJI: EmojiData = EmojiData.builder().id(1437069172772438146L).name("info").build()

    const val INFO = " <:info:1437069172772438146> "
    const val TIME = " <:time:1340019219638652968> "
    const val INFIGHT = " <:infight:1340021072942202981> "
    const val DOWNTIME = " <:downtime:1340021105699717231> "
    const val PULLS = " <:pulls:1340023111227277382> "
    const val SUCCESS = " <:success:1340023129929551913> "
    const val CM_SUCCESS = " <:cmsuccess:1340023147059216404> "
    const val WIPE = " <:wipe:1340023173785194628> "
    const val GROUP_DPS = " <:groupdps:1340055121337389217> "
    const val TOP_STATS = " <:topstats:1340084035812331664> "
    const val DPS = " <:dps:1340084146755735583> "
    const val CC = " <:cc:1340084228120903790> "
    const val RES_TIME = " <:restime:1340084310123872306> "
    const val CONDI_CLEANSE = " <:condicleanse:1340084376662446140> "
    const val BOON_STRIPS = " <:boonstrips:1340084457217982464> "
    const val DMG_TAKEN = " <:damagetaken:1340084800710774814> "
    const val DOWNSTATES = " <:downstates:1340084874027077692> "
    const val HEAL = " <:heal:1340085836510924810> "
    const val BARRIER = " <:barrier:1340085847155806310> "
    const val SEC_TOP_STATS = " <:sectopstats:1340087197541597214> "
    const val WIPES = " <:wipes:1340797369242878028> "
    const val CONDI = " <:condi:1342553033112027239> "
    const val POWER = " <:power:1342553024836665345> "
    const val MIGHT = " <:might:1343681843844874322> "
    const val ARROW_UP = " <:arrow_up:1344763437510692987> "
    const val ARROW_DOWN = " <:arrow_down:1344763483266486452> "

    private val PROFESSIONS = mapOf(
        "guardian" to "<:guardian:1342553720310988871>",
        "dragonhunter" to "<:dragonhunter:1342553707748786186>",
        "firebrand" to "<:firebrand:1342553696596262973>",
        "willbender" to "<:willbender:1342553684780777594>",
        "luminary" to "<:luminary:1436688571342917794>",
        "revenant" to "<:revenant:1342553665436647556>",
        "herald" to "<:herald:1342553652828704808>",
        "renegade" to "<:renegade:1342553640518549534>",
        "vindicator" to "<:vindicator:1342553616862679172>",
        "conduit" to "<:conduit:1436688569308545076>",
        "warrior" to "<:warrior:1342553606078992405>",
        "berserker" to "<:berserker:1342553586810224670>",
        "spellbreaker" to "<:spellbreaker:1342553565066956904>",
        "bladesworn" to "<:bladesworn:1342553555353210962>",
        "paragon" to "<:paragon:1436688568075686031>",
        "engineer" to "<:engineer:1342553529578946631>",
        "scrapper" to "<:scrapper:1342553517373526170>",
        "holosmith" to "<:holosmith:1342553492472070174>",
        "mechanist" to "<:mechanist:1342553456832942110>",
        "amalgam" to "<:amalgam:1436688566670327858>",
        "ranger" to "<:ranger:1342553443981721790>",
        "druid" to "<:druid:1342553432338206782>",
        "soulbeast" to "<:soulbeast:1342553418979610714>",
        "untamed" to "<:untamed:1342553405721149471>",
        "galeshot" to "<:galeshot:1436688565563297923>",
        "thief" to "<:thief:1342553394509778964>",
        "daredevil" to "<:daredevil:1342553378282147840>",
        "deadeye" to "<:deadeye:1342553355888758854>",
        "specter" to "<:specter:1342553347990749395>",
        "antiquary" to "<:antiquary:1436688563835240478>",
        "elementalist" to "<:elementalist:1342553318655918080>",
        "tempest" to "<:tempest:1342553265900093490>",
        "weaver" to "<:weaver:1342553257964208288>",
        "catalyst" to "<:catalyst:1342553247877038193>",
        "evoker" to "<:evoker:1436688572660056197>",
        "mesmer" to "<:mesmer:1342553238087401542>",
        "chronomancer" to "<:chronomancer:1342553207540416512>",
        "mirage" to "<:mirage:1342553197394395278>",
        "virtuoso" to "<:virtuoso:1342553149205909535>",
        "troubadour" to "<:troubadour:1436688562727682049>",
        "necromancer" to "<:necromancer:1342553079886909490>",
        "reaper" to "<:reaper:1342553068574609479>",
        "scourge" to "<:scourge:1342553058571456592>",
        "harbinger" to "<:harbinger:1342553045632028784>",
        "ritualist" to "<:ritualist:1436688561368727582>",
    )

    /**
     * @return Emote for the given [profession]
     */
    fun professionEmote(profession: String?) = profession?.let {
        " ${PROFESSIONS[it.lowercase()]} "
    }
}

object CustomColors {
    val TRANSPARENT_COLOR = Color.of(43, 45, 49)
    val GOLD_COLOR = Color.of(237, 178, 39)
    val SILVER_COLOR = Color.of(130, 138, 146)
    val GREEN_COLOR = Color.of(0, 148, 0)
    val RED_COLOR = Color.of(129, 0, 0)
    val ORANGE_COLOR = Color.of(222, 136, 31)
}
