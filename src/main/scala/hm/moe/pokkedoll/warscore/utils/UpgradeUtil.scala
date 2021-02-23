package hm.moe.pokkedoll.warscore.utils

import java.io.File
import java.util

import hm.moe.pokkedoll.warscore.{Test, WarsCore, WarsCoreAPI}
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.{Inventory, ItemStack}
import org.bukkit.{ChatColor, Material, Sound}

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

@Deprecated
object UpgradeUtil {
  private lazy val plugin = WarsCore.instance

  var configFile: File = _
  var config: FileConfiguration = _

  var cache: Map[String, UpgradeItem] = _

  def reloadConfig(): Unit = {
    val test = new Test("UpgradeUtil.reloadConfig()")
    if (configFile == null) {
      createConfig() match {
        case Success(_) =>
          plugin.getLogger.info("upgrade.ymlの読み込みに成功しました")
        case Failure(e) =>
          e.printStackTrace()
          plugin.getLogger.warning("upgrade.ymlの読み込みに失敗しました")
          return
      }
    }
    val cs = config.getKeys(false)
    cache = cs.asScala.map(f => (f, new UpgradeItem(f, config.getConfigurationSection(f)))).toMap
    test.log()
  }

  def createConfig(): Try[Unit] = {
    configFile = new File(plugin.getDataFolder, "upgrade.yml")
    if (!configFile.exists()) {
      configFile.getParentFile.mkdirs()
      plugin.saveResource("upgrade.yml", false)
    }
    config = new YamlConfiguration
    Try(config.load(configFile))
  }

  def saveConfig(): Unit = {
    config.save(configFile)
  }

  /**
   * 強化の内容を更新する
   *
   * @param upgradeItem upgradeItem
   */
  def setUpgradeItem(upgradeItem: UpgradeItem): Unit = {
    val key = upgradeItem.name
    upgradeItem.list.foreach(f => {
      config.set(s"$key.${f._1}.id", f._2._1)
      config.set(s"$key.${f._1}.chance", f._2._2)
    })
    saveConfig()
    reloadConfig()
  }

  /**
   * 新たに強化を追加する
   */
  def newUpgradeItem(key: String): Unit = {
    config.set(s"$key.else.id", "air")
    config.set(s"$key.else.chance", 100d)
    saveConfig()
    reloadConfig()
  }

  /**
   * 強化を削除する
   *
   * @param key アイテムのキー
   */
  def delUpgradeItem(key: String): Unit = {
    config.set(key, null)
    saveConfig()
    reloadConfig()
  }


  def isUpgradeItem(item: ItemStack): Boolean = {
    cache.contains(ItemUtil.cache.find(p => p._2.isSimilar(item)).getOrElse(return false)._1)
  }

  def getUpgradeItem(item: ItemStack): Option[UpgradeItem] = {
    cache.get(ItemUtil.cache.find(p => p._2.isSimilar(item)).getOrElse(return None)._1)
  }

  val invalidItem: ItemStack = {
    val i = new ItemStack(Material.BARRIER, 1)
    val m = i.getItemMeta
    m.setDisplayName("§cInvalid Item!")
    m.setLore(util.Arrays.asList("§e⚠ このアイテムは無効です"))
    i.setItemMeta(m)
    i
  }

  /**
   * 強化先を決める; upgradesから派生
   */
  class UpgradeItem(val name: String, cs: ConfigurationSection) {
    // 強化先, 派生先アイテムのマップ; アイテムID -> アイテムID, 補正確率
    var list: Map[String, (String, Double)] = cs.getKeys(false)
      .asScala
      .map(c => (c, (cs.getString(s"$c.id"), cs.getDouble(s"$c.chance"))))
      .toMap
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
      false
    } else {
      getUpgradeItem(sourceItem) match {
        case Some(upgradeItem) =>
          val resultItem = inv.getItem(2)
          if (resultItem == null || resultItem.getType == Material.AIR || resultItem.getType == Material.BARRIER) {
            return false
          } else {
            val chance: Double = getChance(resultItem)
            // 成功した場合
            if (WarsCoreAPI.randomChance(chance)) {
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
      true
    }
  }
}
