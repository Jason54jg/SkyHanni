package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AshfangNextResetCooldown {

    private var spawnTime = 1L

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (EntityUtils.getEntities<EntityArmorStand>().any {
                it.posY > 145 && (it.name.contains("§c§9Ashfang Acolyte§r") || it.name.contains("§c§cAshfang Underling§r"))
            }) {
            spawnTime = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (spawnTime == -1L) return

        val remainingTime = spawnTime + 46_100 - System.currentTimeMillis()
        if (remainingTime > 0) {
            val format = TimeUtils.formatDuration(remainingTime, TimeUnit.SECOND, showMilliSeconds = true)
            SkyHanniMod.feature.ashfang.nextResetCooldownPos.renderString(
                "§cAshfang next reset in: §a$format",
                posLabel = "Ashfang Reset Cooldown"
            )
        } else {
            spawnTime = -1
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        spawnTime = -1
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.ashfang.nextResetCooldown &&
                DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }
}