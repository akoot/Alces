package me.akoot.plugins

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import io.papermc.paper.event.block.BlockBreakBlockEvent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class Alces : JavaPlugin(), Listener {

    private lateinit var serializer: GsonComponentSerializer

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        serializer = GsonComponentSerializer.gson()
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block

        val blockData = block.blockData
        if (blockData is Ageable) return

        val pdc = getPDC(block)
        val key = getKey(block.location)

        val itemMeta = event.itemInHand.itemMeta
        val displayName = itemMeta.displayName()?.let { serializer.serialize(it) }
        val lore = itemMeta.lore()?.mapNotNull { serializer.serialize(it) }?.joinToString("\n")

        if (displayName == null && lore == null) return

        pdc.set(key, PersistentDataType.STRING, "${displayName ?: "-"}\n${lore ?: "-"}")
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block

        val pdc = getPDC(block)
        val key = getKey(block.location)

        if (!pdc.has(key, PersistentDataType.STRING)) return

        restoreBlock(block, pdc)
    }


    @EventHandler
    fun onBlockBreakBlock(event: BlockBreakBlockEvent) {
        val block = event.block

        val pdc = getPDC(block)
        val key = getKey(block.location)

        if (!pdc.has(key, PersistentDataType.STRING)) return

        restoreBlock(block, pdc)
        event.drops.clear()
    }

    @EventHandler
    fun onBlockDestroy(event: BlockDestroyEvent) {
        val block = event.block

        val pdc = getPDC(block)
        val key = getKey(block.location)

        if (!pdc.has(key, PersistentDataType.STRING)) return

        restoreBlock(block, pdc)
        event.setWillDrop(false)
    }

    @EventHandler
    fun onExplosion(event: EntityExplodeEvent) {
        val explodedBlocks = event.blockList()

        for (block in explodedBlocks) {
            val pdc = getPDC(block)
            val key = getKey(block.location)

            if (!pdc.has(key, PersistentDataType.STRING)) continue

            restoreBlock(block, pdc)
        }
    }

    private fun restoreBlock(block: Block, pdc: PersistentDataContainer) {

        if (block.drops.isEmpty()) return
        val key = getKey(block.location)
        val data = pdc.get(key, PersistentDataType.STRING) ?: return

        val lines = data.split("\n")
        val displayName = lines[0]
        val lore = lines.drop(1)



        val drop = block.drops.first()
        val itemMeta = drop.itemMeta

        if (displayName != "-")
            itemMeta.displayName(serializer.deserialize(displayName))

        if (lines[1] != "-")
            itemMeta.lore(lore.map { serializer.deserialize(it) })

        drop.itemMeta = itemMeta

        block.location.world.dropItemNaturally(block.location, drop)
        block.type = Material.AIR
        pdc.remove(key)
    }

    private fun getPDC(block: Block): PersistentDataContainer {
        return block.chunk.persistentDataContainer
    }

    private fun getKey(location: Location): NamespacedKey {
        val key = "${location.world.name}.${location.blockX}.${location.blockY}.${location.blockZ}"
        return NamespacedKey("alces", key)
    }
}