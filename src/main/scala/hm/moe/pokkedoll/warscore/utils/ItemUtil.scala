package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.Material
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.Nullable

import java.io.File
import java.util.Optional
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


  def reloadItem(): String = {
    plugin.database.getItems match {
      case Success(value) =>
        cache = value.map(f => (f._1, f._3)).toMap
        "Itemテーブルの読み込みに成功しました"
      case Failure(e) =>
        e.printStackTrace()
        "Itemテーブルの読み込みに失敗しました"
    }
  }

  @Deprecated
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

  def getItemJava(key: String): ItemStack = cache(key)

  /**
   * キーを取得する。効率はあまりよくない
   *
   * @param item アイテムのキー
   * @return
   */
  def getKey(item: ItemStack): String = cache.find(p => p._2.isSimilar(item)).map(_._1).getOrElse("")

  /**
   * アイテムを更新する。関数型言語で書く？
   * @return
   */
  val updateItem: ((String, Option[ItemStack])) => String = (plugin.database.updateItem(_, _)).tupled.andThen({ case Success(_) => reloadItem() case Failure(e) => e.getMessage})

  def getItemName(item: ItemStack): String =
    if (item.hasItemMeta) if (item.getItemMeta.hasDisplayName) item.getItemMeta.getDisplayName else "" else ""
}
