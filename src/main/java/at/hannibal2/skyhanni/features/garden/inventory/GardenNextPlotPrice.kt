package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.name
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenNextPlotPrice {

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!GardenAPI.inGarden()) return
        if (!SkyHanniMod.feature.garden.plotPrice) return

        if (InventoryUtils.openInventoryName() != "Configure Plots") return

        val name = event.itemStack.name ?: return
        if (!name.startsWith("§ePlot")) return

        var next = false
        val list = event.toolTip
        var i = -1
        for (l in list) {
            i++
            val line = l.substring(4)
            if (line.contains("Cost")) {
                next = true
                continue
            }

            if (next) {
                val (itemName, amount) = ItemUtils.readItemAmount(line)
                if (itemName != null) {
                    val lowestBin = NEUItems.getPrice(NEUItems.getRawInternalName(itemName))
                    val price = lowestBin * amount
                    val format = NumberUtil.format(price)
                    list[i] = list[i] + " §7(§6$format§7)"
                } else {
                    LorenzUtils.error("§c[SkyHanni] Could not read item '$line'")
                }
                break
            }
        }
    }
}