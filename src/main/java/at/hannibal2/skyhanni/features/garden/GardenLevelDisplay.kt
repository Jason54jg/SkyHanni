package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class GardenLevelDisplay {
    private val config get() = SkyHanniMod.feature.garden
    private val expToNextLevelPattern = "(?:.*) §e(?<nextLevelExp>.*)§6\\/(?:.*)".toPattern()
    private val overflowPattern = ".*§r §6(?<overflow>.*) XP".toPattern()
    private val namePattern = "Garden Level (?<currentLevel>.*)".toPattern()
    private var gardenExp
        get() = GardenAPI.config?.experience ?: -1
        set(value) {
            GardenAPI.config?.experience = value
        }
    private var display = ""
    private var visitorRewardPattern = " {4}§r§8\\+§r§2(?<exp>.*) §r§7Garden Experience".toPattern()

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        update()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatMessage(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return

        visitorRewardPattern.matchMatcher(event.message) {
            addExp(group("exp").toInt())
        }
    }

    private fun addExp(moreExp: Int) {
        val oldLevel = GardenAPI.getLevelForExp(gardenExp.toLong())
        gardenExp += moreExp
        val newLevel = GardenAPI.getLevelForExp(gardenExp.toLong())
        if (newLevel == oldLevel + 1) {
            if (newLevel > 15) {
                LorenzUtils.runDelayed(50.milliseconds) {
                    LorenzUtils.chat(
                        " \n§b§lGARDEN LEVEL UP §8$oldLevel ➜ §b$newLevel\n" +
                                " §8+§aRespect from Elite Farmers and SkyHanni members :)\n "
                    )
                }
            }
        }
        update()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!GardenAPI.inGarden()) return
        if (event.inventoryName != "Desk") return
        val item = event.inventoryItems[4]!!

        namePattern.matchMatcher(item.name!!.removeColor()) {
            val currentLevel = group("currentLevel").romanToDecimalIfNeeded()
            var nextLevelExp = 0
            for (line in item.getLore()) {
                expToNextLevelPattern.matchMatcher(line) {
                    nextLevelExp = group("nextLevelExp").replace(",", "").toDouble().roundToInt()
                }
                overflowPattern.matchMatcher(line) {
                    val overflow = group("overflow").replace(",", "").toDouble().roundToInt()
                    gardenExp = overflow
                    update()
                    return
                }
            }
            val expForLevel = GardenAPI.getExpForLevel(currentLevel).toInt()
            gardenExp = expForLevel + nextLevelExp
            update()
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): String {
        if (gardenExp == -1) return "§aGarden Level ? §cOpen the desk!"
        val currentLevel = GardenAPI.getLevelForExp(gardenExp.toLong())
        val needForLevel = GardenAPI.getExpForLevel(currentLevel).toInt()
        val nextLevel = currentLevel + 1
        val needForNextLevel = GardenAPI.getExpForLevel(nextLevel).toInt()

        return "§aGarden Level $currentLevel" + if (needForNextLevel != 0) {
            val overflow = gardenExp - needForLevel
            val needForOnlyNextLvl = needForNextLevel - needForLevel

            val need = LorenzUtils.formatInteger(overflow)
            val have = LorenzUtils.formatInteger(needForOnlyNextLvl)
            " §7(§e$need§7/§e$have§7)"
        } else ""
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.gardenLevelPos.renderString(display, posLabel = "Garden Level")
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.gardenLevelDisplay
}