package org.inquest.discord.isac

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.temporal.Temporal
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration

fun Duration.inRoundedMinutes() = (inWholeSeconds / 60.0).roundToInt()

fun Double.format(pattern: String) =
    DecimalFormat(pattern, DecimalFormatSymbols(Locale.GERMAN)).format(this)

fun Int.padded(padding: Int) = "%1$${padding}s".format(this)

fun Long.padded(padding: Int) = "%1$${padding}s".format(this)

fun Temporal.toDiscordTimestamp() = "<t:${Instant.from(this).epochSecond}>"

fun StringBuilder.appendTimestamp(temporal: Temporal) = append(" ${temporal.toDiscordTimestamp()} ")

fun StringBuilder.space() = append(' ')

fun StringBuilder.appendBold(any: Any) = append("**$any**")

fun StringBuilder.appendItalic(any: Any) = append("_${any}_")

fun StringBuilder.appendMono(any: Any) = append("``$any``")

fun StringBuilder.appendUnderlined(any: Any) = append("__${any}__")

object CustomEmojis {
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
}
