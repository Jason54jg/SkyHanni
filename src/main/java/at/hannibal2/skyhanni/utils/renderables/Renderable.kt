package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NEUItems.renderOnScreen
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse
import kotlin.math.max

interface Renderable {
    val width: Int
    val height: Int
    fun isHovered(posX: Int, posY: Int) =
        Utils.getMouseX() in (posX..posX + width)
            && Utils.getMouseY() in (posY..posY + height) // TODO: adjust for variable height?

    /**
     * N.B.: the offset is absolute, not relative to the position and shouldn't be used for rendering
     * (the GL matrix stack should already be pre transformed)
     */
    fun render(posX: Int, posY: Int)

    companion object {
        val logger = LorenzLogger("debug/renderable")
        val list = mutableMapOf<Pair<Int, Int>, List<Int>>()

        fun fromAny(any: Any?, itemScale: Double = 1.0): Renderable? = when (any) {
            null -> placeholder(12)
            is Renderable -> any
            is String -> string(any)
            is ItemStack -> itemStack(any, itemScale)
            else -> null
        }

        fun link(text: String, bypassChecks: Boolean = false, onClick: () -> Unit): Renderable =
            link(string(text), onClick, bypassChecks = bypassChecks) { true }

        fun optionalLink(
            text: String,
            onClick: () -> Unit,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true }
        ): Renderable =
            link(string(text), onClick, bypassChecks, condition)

        fun link(
            renderable: Renderable,
            onClick: () -> Unit,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true }
        ): Renderable {
            return clickable(
                hoverable(underlined(renderable), renderable, bypassChecks, condition = condition),
                onClick,
                0,
                bypassChecks,
                condition
            )
        }

        fun clickAndHover(
            text: String,
            tips: List<String>,
            bypassChecks: Boolean = false,
            onClick: () -> Unit
        ): Renderable {
            return clickable(hoverTips(text, tips, bypassChecks = bypassChecks), onClick, bypassChecks = bypassChecks)
        }

        fun clickable(
            render: Renderable,
            onClick: () -> Unit,
            button: Int = 0,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true }
        ) =
            object : Renderable {
                override val width: Int
                    get() = render.width
                override val height = 10

                private var wasDown = false

                override fun render(posX: Int, posY: Int) {
                    val isDown = Mouse.isButtonDown(button)
                    if (isDown > wasDown && isHovered(posX, posY)) {
                        if (condition() && shouldAllowLink(true, bypassChecks)) {
                            onClick()
                        }
                    }
                    wasDown = isDown
                    render.render(posX, posY)
                }
            }

        fun hoverTips(text: String, tips: List<String>, indexes: List<Int> = listOf(), stack: ItemStack? = null, bypassChecks: Boolean = false, condition: () -> Boolean = { true }): Renderable {

            val render = string(text)
            return object : Renderable {
                override val width: Int
                    get() = render.width
                override val height = 11

                override fun render(posX: Int, posY: Int) {
                    render.render(posX, posY)
                    if (isHovered(posX, posY)) {
                        if (condition() && shouldAllowLink(true, bypassChecks)) {
                            list[Pair(posX, posY)] = indexes
                            GlStateManager.pushMatrix()
                            GlStateManager.translate(0F, 0F, 400F)
                            RenderLineTooltips.drawHoveringText(posX, posY, tips, stack)
                            GlStateManager.popMatrix()
                        }
                    } else {
                        if (list.contains(Pair(posX, posY))) {
                            list.remove(Pair(posX, posY))
                        }
                    }
                }
            }
        }

        private fun shouldAllowLink(debug: Boolean = false, bypassChecks: Boolean): Boolean {
            val isGuiScreen = Minecraft.getMinecraft().currentScreen != null
            if (bypassChecks) {
                return isGuiScreen
            }
            val isGuiPositionEditor = Minecraft.getMinecraft().currentScreen !is GuiPositionEditor
            val isNotInSignAndOnSlot = if (Minecraft.getMinecraft().currentScreen !is GuiEditSign) {
                ToolTipData.lastSlot == null
            } else true
            val isConfigScreen = Minecraft.getMinecraft().currentScreen !is GuiScreenElementWrapper
            val result = isGuiScreen && isGuiPositionEditor && isNotInSignAndOnSlot && isConfigScreen

            if (debug) {
                if (!result) {
                    logger.log("")
                    logger.log("blocked link because:")
                    if (!isGuiScreen) logger.log("isGuiScreen")
                    if (!isGuiPositionEditor) logger.log("isGuiPositionEditor")
                    if (!isNotInSignAndOnSlot) logger.log("isNotInSignAndOnSlot")
                    if (!isConfigScreen) logger.log("isConfigScreen")
                    logger.log("")
                } else {
                    logger.log("allowed click")
                }
            }

            return result
        }

        fun underlined(renderable: Renderable) = object : Renderable {
            override val width: Int
                get() = renderable.width
            override val height = 10

            override fun render(posX: Int, posY: Int) {
                Gui.drawRect(0, 10, width, 11, 0xFFFFFFFF.toInt())
                GlStateManager.color(1F, 1F, 1F, 1F)
                renderable.render(posX, posY)
            }
        }

        fun hoverable(
            hovered: Renderable,
            unhovered: Renderable,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true }
        ) =
            object : Renderable {
                override val width: Int
                    get() = max(hovered.width, unhovered.width)
                override val height = 10

                override fun render(posX: Int, posY: Int) {
                    if (isHovered(posX, posY) && condition() && shouldAllowLink(true, bypassChecks))
                        hovered.render(posX, posY)
                    else
                        unhovered.render(posX, posY)
                }
            }

        fun itemStack(any: ItemStack, scale: Double = 1.0) = object : Renderable {
            override val width: Int
                get() = 12
            override val height = 10

            override fun render(posX: Int, posY: Int) {
                GlStateManager.pushMatrix()
                if (Minecraft.getMinecraft().currentScreen == null || Minecraft.getMinecraft().currentScreen is GuiChat)
                    GlStateManager.translate(0F, 0F, -145F)
                any.renderOnScreen(0F, 0F, scaleMultiplier = scale)
                GlStateManager.popMatrix()
            }
        }

        fun string(string: String) = object : Renderable {
            override val width: Int
                get() = Minecraft.getMinecraft().fontRendererObj.getStringWidth(string)
            override val height = 10

            override fun render(posX: Int, posY: Int) {
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow("§f$string", 1f, 1f, 0)
            }
        }

        fun placeholder(width: Int) = object : Renderable {
            override val width: Int = width
            override val height = 10

            override fun render(posX: Int, posY: Int) {
            }
        }
    }
}