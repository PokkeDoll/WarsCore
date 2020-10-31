package hm.moe.pokkedoll.warscore.utils

import java.io.File

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.Material
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.inventory.ItemStack

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

/**
 * アイテムを管理するオブジェクト
 * さらに効率を追求！
 *
 * @author Emorard
 * @version 4
 */
object ItemUtil {
  var cache: Map[String, ItemStack] = _

  val invalid = new ItemStack(Material.STONE, 1)

  private lazy val plugin = WarsCore.instance

  var configFile: File = _
  var config: FileConfiguration = _

  def reloadItem(): Unit = {
    if (configFile == null) {
      createConfig() match {
        case Success(_) =>
          plugin.getLogger.info("item.ymlの読み込みに成功しました")
        case Failure(e) =>
          e.printStackTrace()
          plugin.getLogger.warning("item.ymlの読み込みに失敗しました")
          return
      }
    }
    val cs = config.getKeys(false)
    cache = cs.asScala.map(f => (f, config.getItemStack(f))).toMap
  }

  def createConfig(): Try[Unit] = {
    configFile = new File(plugin.getDataFolder, "item.yml")
    if (!configFile.exists()) {
      configFile.getParentFile.mkdirs()
      plugin.saveResource("item.yml", false)
    }
    config = new YamlConfiguration
    Try(config.load(configFile))
  }

  /**
   * キャッシュ無しのメソッド
   *
   * @param key アイテムのキー
   * @return
   */
  def getItem(key: String): Option[ItemStack] = cache.get(key)

  /**
   * キーを取得する。効率はあまりよくない
   *
   * @param item アイテムのキー
   * @return
   */
  def getKey(item: ItemStack): String = cache.find(p => p._2.isSimilar(item)).map(_._1).getOrElse("")


  def setItem(key: String, item: ItemStack): Unit = {
    val c = item.clone()
    config.set(key, c)
    config.save(configFile)
    reloadItem()
  }

  def removeItem(key: String): Unit = {
    config.set(key, null)
    config.save(configFile)
    reloadItem()
  }

  def getItemName(item: ItemStack): String =
    if (item.hasItemMeta) if (item.getItemMeta.hasDisplayName) item.getItemMeta.getDisplayName else "" else ""
}
