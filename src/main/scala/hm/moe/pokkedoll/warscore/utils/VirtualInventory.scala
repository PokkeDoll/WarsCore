package hm.moe.pokkedoll.warscore.utils

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * PlayerInventoryが普通に使えないことに怒ったインベントリ
 */
class VirtualInventory {
  // 多分 36
  var storage: Array[ItemStack] = _
  // 多分 4
  var armor: Array[ItemStack] = _
  // 多分 1
  var extra: Array[ItemStack] = _
}

object VirtualInventory {
  def to(inv: VirtualInventory): String = {
    if(inv == null) " " else {
      val yaml = new YamlConfiguration
      inv.storage.indices.foreach(i => yaml.set(s"storage.${i}", inv.storage(i)))
      inv.armor.indices.foreach(i => yaml.set(s"armor.${i}", inv.armor(i)))
      inv.extra.indices.foreach(i => yaml.set(s"extra.${i}", inv.extra(i)))
      yaml.saveToString()
    }
  }

  def from(string: String): VirtualInventory = {
    val inv = new VirtualInventory
    val yaml = new YamlConfiguration
    try {
      yaml.loadFromString(string)
      val storage = yaml.getConfigurationSection("storage")
      val armor = yaml.getConfigurationSection("armor")
      val extra = yaml.getConfigurationSection("extra")

      inv.storage = (0 to storage.getKeys(false).size()).map(i => storage.getItemStack(i.toString)).toArray
      inv.armor = (0 to armor.getKeys(false).size()).map(i => armor.getItemStack(i.toString)).toArray
      inv.extra = (0 to armor.getKeys(false).size()).map(i => extra.getItemStack(i.toString)).toArray

      inv
    } catch {
      case e: Exception =>
        e.printStackTrace()
        inv
    }
  }

  def empty(): VirtualInventory = {
    val inv = new VirtualInventory
    inv.storage = new Array[ItemStack](36)
    inv.armor = new Array[ItemStack](4)
    inv.extra = new Array[ItemStack](1)
    inv
  }
}
