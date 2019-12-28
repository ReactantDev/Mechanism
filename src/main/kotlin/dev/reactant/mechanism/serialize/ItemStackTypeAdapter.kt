package dev.reactant.mechanism.serialize

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dev.reactant.reactant.utils.content.item.itemStackOf
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack

class ItemStackTypeAdapter : TypeAdapter<ItemStack>() {
    override fun write(writer: JsonWriter, value: ItemStack) {
        when {
            value.type.isAir -> writer.nullValue()
            else -> writer.value(YamlConfiguration().also { it["itemstack"] = value }.saveToString())
        }
    }

    override fun read(reader: JsonReader): ItemStack = when (reader.peek()) {
        JsonToken.NULL -> reader.nextNull().let { itemStackOf(Material.AIR) }
        else -> reader.nextString().let { YamlConfiguration().apply { loadFromString(it) } }
                .getItemStack("itemstack") ?: itemStackOf(Material.AIR)
    }

}
