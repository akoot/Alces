package me.akoot.plugins

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
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
        if (!isHead(block)) return

        val key = getKey(block.location)
        val pdc = block.chunk.persistentDataContainer

        val itemMeta = event.itemInHand.itemMeta
        val displayName = itemMeta.displayName()?.let { serializer.serialize(it) }
        val lore = itemMeta.lore()?.mapNotNull { serializer.serialize(it) }?.joinToString("\n")

        if (displayName == null && lore == null) return

        pdc.set(key, PersistentDataType.STRING, "${displayName ?: "-"}\n${lore ?: "-"}")
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        if (!isHead(block)) return

        val key = getKey(block.location)
        val pdc = block.chunk.persistentDataContainer
        val data = pdc.get(key, PersistentDataType.STRING) ?: return

        event.isDropItems = false

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
        pdc.remove(key)
    }

    private fun isHead(block: Block): Boolean {
        return block.type.name.endsWith("_HEAD")
    }

    private fun getKey(location: Location): NamespacedKey {
        val key = "${location.world.name}.${location.blockX}.${location.blockY}.${location.blockZ}"
        return NamespacedKey("alces", key)
    }
}