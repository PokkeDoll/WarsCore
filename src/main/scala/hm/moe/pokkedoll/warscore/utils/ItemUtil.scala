package hm.moe.pokkedoll.warscore.utils

import java.io.File

import hm.moe.pokkedoll.warscore.{Test, WarsCore}
import org.bukkit.Material
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.inventory.ItemStack

import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

/**
 * アイテムを管理するオブジェクト
 *
 * @author Emorard
 * @version 3
 */
object ItemUtil {
  @Deprecated
  val items = mutable.HashMap.empty[String, ItemStack]

  val itemCache = mutable.HashMap.empty[String, ItemStack]

  val invalid = new ItemStack(Material.STONE, 1)

  private lazy val plugin = WarsCore.instance

  var config: FileConfiguration = _

  def reload(): Unit = {
    createConfig() match {
      case Success(_) =>
        itemCache.clear()
      case Failure(exception) =>
        exception.printStackTrace()
        plugin.getLogger.warning("item.ymlの読み込みに失敗しました")
    }
  }

  def createConfig(): Try[Unit] = {
    val file = new File(plugin.getDataFolder, "item.yml")
    if (!file.exists()) {
      file.getParentFile.mkdirs()
      plugin.saveResource("item.yml", false)
    }
    config = new YamlConfiguration
    Try(config.load(file))
  }



  /**
   * キャッシュを少し変えたメソッド
   * 速度はv1より落ちるがヒープを無駄に使うことがない
   *
   * @param key
   * @return
   */
  def getItem(key: String): Option[ItemStack] =
    if (itemCache.contains(key))
      itemCache.get(key)
    else if (config.isItemStack(key))
      Some(config.getItemStack(key))
    else
      None

  /**
   * アイテムからキーを見つける
   * 速度はv1よりはるかに落ちるがヒープを無駄に使うことがない
   * @param item
   * @return
   */
  def getKey(item: ItemStack): String = {
    val test = new Test("ItemUtil.getItem")
    itemCache.find(p => p._2.isSimilar(item)).map(_._1) match {
      case Some(key) =>
        test.log(1)
        key
      case None =>
        config.getKeys(false)
          .asScala
          .find(p => config.getItemStack(p).isSimilar(item)) match {
          case Some(key) =>
            itemCache.put(key, config.getItemStack(key))
            test.log(2)
            key
          case None =>
            test.log(2)
            ""
        }
    }
  }

  def set(key: String, item: ItemStack): Unit = {
    val c = item.clone()
    itemCache.put(key, c)
    config.set(key, c)
    config.save("item.yml")
  }

  def remove(key: String): Unit = {
    itemCache.remove(key)
    config.set(key, null)
    config.save("item.yml")
  }

  def getItemName(item: ItemStack): String =
    if (item.hasItemMeta) if (item.getItemMeta.hasDisplayName) item.getItemMeta.getDisplayName else "" else ""

  @Deprecated
  def reloadItem(): Unit = {
    items.clear()
    plugin.reloadConfig()
    val config = plugin.getConfig
    if (config.isConfigurationSection("items")) {
      config.getConfigurationSection("items").getKeys(false).forEach(key => {
        items.put(key, config.getItemStack(s"items.$key", invalid))
      })
    }
  }

  @Deprecated
  def saveItem(): Unit = {
    plugin.saveConfig()
  }

  @Deprecated
  def setItem(key: String, item: ItemStack): Unit = {
    items.put(key, item)
    plugin.getConfig.set(s"items.$key", item)
  }

  @Deprecated
  def removeItem(key: String): Unit = {
    items.remove(key)
    plugin.getConfig.set(s"items.$key", null)
  }

  @Deprecated
  def getItemKey(item: ItemStack): String = {
    items.find(p => p._2.isSimilar(item)) match {
      case Some(v) => v._1
      case _ => ""
    }
  }
}
