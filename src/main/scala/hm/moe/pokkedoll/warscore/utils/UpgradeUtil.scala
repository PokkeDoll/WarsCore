package hm.moe.pokkedoll.warscore.utils

import java.util

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.{ChatColor, Material, Sound}
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.{Inventory, ItemStack}

import scala.collection.mutable
import scala.util.Try

object UpgradeUtil {
  val routes = mutable.HashMap.empty[String, UpgradeItem]

  private lazy val plugin = WarsCore.instance

  /**
   * こっちの方が効率的!
   *
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
   *
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
    println(s"§aUpgradeUtil#reload() took ${dE - dS} ms!")
  }

  def isUpgradeItem(item: ItemStack): Boolean = {
    routes.contains(ItemUtil.cache.find(p => p._2.isSimilar(item)).getOrElse(return false)._1)
    false
  }

  def getUpgradeItem(item: ItemStack): Option[UpgradeItem] = {
    routes.get(ItemUtil.cache.find(p => p._2.isSimilar(item)).getOrElse(return None)._1)
  }

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
   *
   * @return
   */
  def getChance(item: ItemStack): Double = {
    val meta = item.getItemMeta
    val lore = if (meta.hasLore) meta.getLore else return 0d
    val chance = lore.stream().filter(_.startsWith(ChatColor.WHITE + "成功確率: " + ChatColor.GREEN)).findFirst()
    if (chance.isPresent) Try(chance.get()
      .replaceAll(ChatColor.WHITE + "成功確率: " + ChatColor.GREEN, "")
      .replaceAll("%", "")
      .toDouble).getOrElse(0d) else 0d
  }


  def onUpgrade(inv: Inventory, p: HumanEntity): Boolean = {
    val sourceItem = inv.getItem(0)
    val materialItem = inv.getItem(1)
    if (sourceItem == null || materialItem == null) {
      return false
    } else {
      getUpgradeItem(sourceItem) match {
        case Some(upgradeItem) =>
          val resultItem = inv.getItem(2)
          if (resultItem == null || resultItem.getType == Material.AIR) {
            return false
          } else {
            val chance: Double = getChance(resultItem)
            // 成功した場合
            if(WarsCoreAPI.randomChance(chance)) {
              inv.setItem(0, new ItemStack(Material.AIR))
              inv.setItem(1, new ItemStack(Material.AIR))
              inv.setItem(2, new ItemStack(Material.AIR))
              // 遅い(前よりは改善したけど
              val key = ItemUtil.getKey(materialItem)
              val success = if (upgradeItem.list.contains(key)) upgradeItem.list.get(key) else upgradeItem.list.get("else")
              ItemUtil.getItem(success.getOrElse(("", 0.0))._1) match {
                case Some(value) =>
                  p.setItemOnCursor(value)
                  p.sendMessage("§9成功しました!")
                  p.getWorld.playSound(p.getLocation, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f)
                  p.getWorld.playSound(p.getLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f)
                  WarsCoreAPI.spawnFirework(p.getLocation)
                case None =>
              }
            } else {
              p.sendMessage(ChatColor.RED + "失敗しました...")
              inv.setItem(1, new ItemStack(Material.AIR))
              inv.setItem(2, new ItemStack(Material.AIR))
              p.getWorld.playSound(p.getLocation, Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f)
            }
          }
        case None =>
      }
      return true
    }
  }
}
