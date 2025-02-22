package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay.Companion.getAbilityFortune
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.DecimalFormat
import kotlin.math.roundToInt

class ToolTooltipTweaks {
    private val config get() = SkyHanniMod.feature.garden
    private val tooltipFortunePattern =
        "^§5§o§7Farming Fortune: §a\\+([\\d.]+)(?: §2\\(\\+\\d\\))?(?: §9\\(\\+(\\d+)\\))?$".toRegex()
    private val counterStartLine = setOf("§5§o§6Logarithmic Counter", "§5§o§6Collection Analysis")
    private val reforgeEndLine = setOf("§5§o", "§5§o§7chance for multiple crops.")
    private val abilityDescriptionStart = "§5§o§7These boots gain §a+2❈ Defense"
    private val abilityDescriptionEnd = "§5§o§7Skill level."

    private val statFormatter = DecimalFormat("0.##")

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val itemStack = event.itemStack
        val crop = itemStack.getCropType()
        val toolFortune = FarmingFortuneDisplay.getToolFortune(itemStack)
        val counterFortune = FarmingFortuneDisplay.getCounterFortune(itemStack)
        val collectionFortune = FarmingFortuneDisplay.getCollectionFortune(itemStack)
        val turboCropFortune = FarmingFortuneDisplay.getTurboCropFortune(itemStack, crop)
        val dedicationFortune = FarmingFortuneDisplay.getDedicationFortune(itemStack, crop)

        val reforgeName = itemStack.getReforgeName()?.firstLetterUppercase()

        val sunderFortune = FarmingFortuneDisplay.getSunderFortune(itemStack)
        val harvestingFortune = FarmingFortuneDisplay.getHarvestingFortune(itemStack)
        val cultivatingFortune = FarmingFortuneDisplay.getCultivatingFortune(itemStack)
        val abilityFortune = getAbilityFortune(itemStack)

        val ffdFortune = itemStack.getFarmingForDummiesCount() ?: 0
        val hiddenFortune =
            (toolFortune + counterFortune + collectionFortune + turboCropFortune + dedicationFortune + abilityFortune)
        val iterator = event.toolTip.listIterator()

        var removingFarmhandDescription = false
        var removingCounterDescription = false
        var removingReforgeDescription = false
        var removingAbilityDescription = false

        for (line in iterator) {
            val match = tooltipFortunePattern.matchEntire(line)?.groups
            if (match != null) {
                val enchantmentFortune = sunderFortune + harvestingFortune + cultivatingFortune

                FarmingFortuneDisplay.loadFortuneLineData(itemStack, enchantmentFortune)

                val displayedFortune = FarmingFortuneDisplay.displayedFortune
                val reforgeFortune = FarmingFortuneDisplay.reforgeFortune
                val baseFortune = FarmingFortuneDisplay.itemBaseFortune
                val greenThumbFortune = FarmingFortuneDisplay.greenThumbFortune

                val totalFortune = displayedFortune + hiddenFortune


                val ffdString = if (ffdFortune != 0) " §2(+${ffdFortune.formatStat()})" else ""
                val reforgeString = if (reforgeFortune != 0.0) " §9(+${reforgeFortune.formatStat()})" else ""
                val cropString = if (hiddenFortune != 0.0) " §6[+${hiddenFortune.roundToInt()}]" else ""

                val fortuneLine = when (config.cropTooltipFortune) {
                    0 -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString"
                    1 -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString$cropString"
                    else -> "§7Farming Fortune: §a+${totalFortune.formatStat()}$ffdString$reforgeString$cropString"
                }
                iterator.set(fortuneLine)

                if (OSUtils.isKeyHeld(config.fortuneTooltipKeybind)) {
                    iterator.addStat("  §7Base: §6+", baseFortune)
                    iterator.addStat("  §7Tool: §6+", toolFortune)
                    iterator.addStat("  §7${reforgeName?.removeColor()}: §9+", reforgeFortune)
                    iterator.addStat("  §7Ability: §2+", abilityFortune)
                    iterator.addStat("  §7Green Thumb: §a+", greenThumbFortune)
                    iterator.addStat("  §7Sunder: §a+", sunderFortune)
                    iterator.addStat("  §7Harvesting: §a+", harvestingFortune)
                    iterator.addStat("  §7Cultivating: §a+", cultivatingFortune)
                    iterator.addStat("  §7Farming for Dummies: §2+", ffdFortune)
                    iterator.addStat("  §7Counter: §6+", counterFortune)
                    iterator.addStat("  §7Collection: §6+", collectionFortune)
                    iterator.addStat("  §7Dedication: §6+", dedicationFortune)
                    iterator.addStat("  §7Turbo-Crop: §6+", turboCropFortune)
                }
            }
            // Beware, dubious control flow beyond these lines
            if (config.compactToolTooltips || FFGuideGUI.isInGui()) {
                if (line.startsWith("§5§o§7§8Bonus ")) removingFarmhandDescription = true
                if (removingFarmhandDescription) {
                    iterator.remove()
                    removingFarmhandDescription = line != "§5§o"
                }

                if (removingCounterDescription && !line.startsWith("§5§o§7You have")) {
                    iterator.remove()
                } else {
                    removingCounterDescription = false
                }
                if (counterStartLine.contains(line)) removingCounterDescription = true

                if (line == "§5§o§9Blessed Bonus") removingReforgeDescription = true
                if (removingReforgeDescription) {
                    iterator.remove()
                    removingReforgeDescription = !reforgeEndLine.contains(line)
                }
                if (line == "§5§o§9Bountiful Bonus") removingReforgeDescription = true

                if (FFGuideGUI.isInGui()) {
                    if (line.contains("Click to ") || line.contains("§7§8This item can be reforged!") || line.contains("Dyed")) {
                        iterator.remove()
                    }

                    if (line == abilityDescriptionStart) {
                        removingAbilityDescription = true
                    }
                    if (removingAbilityDescription) {
                        iterator.remove()
                        if (line == abilityDescriptionEnd) {
                            removingAbilityDescription = false
                        }
                    }
                }
            }
        }

        // Fixing a hypixel bug. TODO remove once hypixel fixes it. use disabled features repo maybe?
        if (itemStack.getInternalName().contains("LOTUS")) {
            event.toolTip.replaceAll { it.replace("Kills:", "Visitors:") }
        }
    }

    private fun Number.formatStat() = statFormatter.format(this)

    private fun MutableListIterator<String>.addStat(description: String, value: Number) {
        if (value.toDouble() != 0.0) {
            add("$description${value.formatStat()}")
        }
    }
}