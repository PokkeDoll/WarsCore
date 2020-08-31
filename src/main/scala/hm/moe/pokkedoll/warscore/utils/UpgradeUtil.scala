package hm.moe.pokkedoll.warscore.utils

import java.util

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

import scala.collection.mutable

object UpgradeUtil {
  val routes = mutable.HashMap.empty[String, UpgradeItem]

  private lazy val plugin = WarsCore.instance

  /**
   * こっちの方が効率的!
   * @param upgradeItem
   */
  def updateUpgradeItem(upgradeItem: UpgradeItem): Unit = {
    val config = plugin.getConfig
    val t = s"upgrades.${upgradeItem.name}"
    config.set(t, null)
    upgradeItem.list.foreach(f => {
      config.set(s"$t.${f._1}.id", f._2._1)
      config.set(s"$t.${f._1}.chance", f._2._2)
    })
    plugin.saveConfig()
    reload()
  }
  /**
   * 別に文字列でもいいんだけど
   * @param upgradeItem
   */
  def removeUpgradeItem(upgradeItem: UpgradeItem): Unit = {
    val config = plugin.getConfig
    // つまり削除
    config.set(s"upgrades.${upgradeItem.name}", null)
    plugin.saveConfig()
    reload()
  }

  def removeUpgradeItem(key: String): Unit = {
    val config = plugin.getConfig
    config.set(s"upgrades.$key", null)
    plugin.saveConfig()
    reload()
  }

  def createUpgradeItem(key: String): Unit = {
    val config = plugin.getConfig
    config.set(s"upgrades.$key.else.id", "air")
    config.set(s"upgrades.$key.else.chance", 100.0)
    plugin.saveConfig()
    reload()
  }

  def reload(): Unit = {
    val dS = System.currentTimeMillis()
    routes.clear()
    val cs = plugin.getConfig.getConfigurationSection("upgrades")
    cs.getKeys(false).forEach(key => routes.put(key, new UpgradeItem(key, cs.getConfigurationSection(key))))
    val dE = System.currentTimeMillis()
    println(s"§aUpgradeUtil#reload() took ${dE-dS} ms!")
  }

  def isUpgradeItem(item: ItemStack): Boolean =
    routes.contains(ItemUtil.items.find(p => p._2.isSimilar(item)).getOrElse(return false)._1)

  def getUpgradeItem(item: ItemStack): Option[UpgradeItem] =
    routes.get(ItemUtil.items.find(p=>p._2.isSimilar(item)).getOrElse(return None)._1)

  reload()

  val invalidItem: ItemStack = {
    val i = new ItemStack(Material.STONE, 1)
    val m = i.getItemMeta
    m.setDisplayName("§cInvalid Item!")
    m.setLore(util.Arrays.asList("§e⚠ このアイテムは無効です"))
    i.setItemMeta(m)
    i
  }

  /**
   * 強化先を決める; upgradesから派生
   */
  class UpgradeItem(val name: String) {
    // 強化先, 派生先アイテムのマップ; アイテムID -> アイテムID, 補正確率
    val list = mutable.HashMap.empty[String, (String, Double)]

    def this(name: String, cs: ConfigurationSection) {
      this(name)
      cs.getKeys(false).forEach(c => {
        list.put(c, (cs.getString(s"$c.id"), cs.getDouble(s"$c.chance")))
      })
    }
  }

  /**
   * アイテムから成功確率を呼び出す
   * @return
   */
  def getChance(item: ItemStack): Double = {
    val meta = item.getItemMeta
    val lore = if(meta.hasLore) meta.getLore else return 0.0
    val chance = lore.stream().filter(f => f.startsWith("§f成功確率: §a")).findFirst()
    if(chance.isPresent) {
      chance.get().replaceAll("§f成功確率: §a", "").replaceAll("%", "").toDouble
    } else {
      0.0
    }
  }
}
