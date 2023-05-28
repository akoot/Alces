package me.akoot.plugins

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.Files

class Alces : JavaPlugin(), Listener {

    private lateinit var serializer: GsonComponentSerializer
    private val storageFile = File(dataFolder, "placed")

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        serializer = GsonComponentSerializer.gson()
        storageFile.mkdirs()
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        if (!isHead(block)) return // but what if all blocks?
        val itemMeta = event.itemInHand.itemMeta
        val displayName = itemMeta.displayName()?.let { serializer.serialize(it) }
        val lore = itemMeta.lore()?.let { it.mapNotNull { line -> serializer.serialize(line) } }?.joinToString("\n")
        if (displayName == null && lore == null) return
        println("writing file")
        Files.writeString(getJsonFile(block.location).toPath(), "${displayName ?: "-"}\n${lore ?: "-"}")
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        if (!isHead(block)) return

        val blockFile = getJsonFile(block.location)
        if (!blockFile.exists()) return

        event.isDropItems = false

        val lines = blockFile.readLines()
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
        blockFile.delete()
    }

    private fun getJsonFile(location: Location): File {
        return File(storageFile, "${location.world.name},${location.blockX},${location.blockY},${location.blockZ}.json")
    }

    private fun isHead(block: Block): Boolean {
        return block.type.name.endsWith("_HEAD")
    }
}