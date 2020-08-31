package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

import scala.collection.mutable

object ItemUtil {
  val items = mutable.HashMap.empty[String, ItemStack]

  val invalid = new ItemStack(Material.STONE, 1)

  private lazy val plugin = WarsCore.instance
  def reloadItem(): Unit = {
    items.clear()
    plugin.reloadConfig()
    if (plugin.getConfig.isConfigurationSection("items")) {
      plugin.getConfig.getConfigurationSection("items").getKeys(false).forEach(key => {
        items.put(key, plugin.getConfig.getItemStack(s"items.$key", invalid))
      })
    }
  }

  def saveItem(): Unit = {
    plugin.saveConfig()
  }

  def setItem(key: String, item: ItemStack): Unit = {
    items.put(key, item)
    plugin.getConfig.set(s"items.$key", item)
  }

  def removeItem(key: String): Unit = {
    items.remove(key)
    plugin.getConfig.set(s"items.$key", null)
  }

  def getItemName(item: ItemStack): String =
    if(item.hasItemMeta) if(item.getItemMeta.hasDisplayName) item.getItemMeta.getDisplayName else "" else ""

  def getItemKey(item: ItemStack): String = {
    items.find(p => p._2.isSimilar(item)) match {
      case Some(v) => v._1
      case _ => ""
    }
  }
}
