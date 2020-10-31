package hm.moe.pokkedoll.warscore.utils

import java.io.File
import java.util.Optional

import hm.moe.pokkedoll.warscore.{Test, WarsCore, WarsCoreAPI}
import org.bukkit.ChatColor
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

object TagUtil {
  private lazy val plugin = WarsCore.instance

  var configFile: File = _
  var config: FileConfiguration = _

  /**
   * コンフィグから取得される、有効なタグのキャッシュ
   */
  var cache: Map[String, String] = _

  def reloadConfig(): Unit = {
    val test = new Test("TagUtil.reloadConfig()")
    if (configFile == null) {
      createConfig() match {
        case Success(_) =>
          plugin.getLogger.info("tag.ymlの読み込みに成功しました")
        case Failure(e) =>
          e.printStackTrace()
          plugin.getLogger.warning("tag.ymlの読み込みに失敗しました")
          return
      }
    }
    val cs = config.getKeys(false)
    cache = cs.asScala.map(f => (f, ChatColor.translateAlternateColorCodes('&', config.getString(f, "null")))).toMap
    test.log()
  }

  def createConfig(): Try[Unit] = {
    configFile = new File(plugin.getDataFolder, "tag.yml")
    if (!configFile.exists()) {
      configFile.getParentFile.mkdirs()
      plugin.saveResource("tag.yml", false)
    }
    config = new YamlConfiguration
    Try(config.load(configFile))
  }

  def saveConfig(): Unit = {
    config.save(configFile)
  }

  /**
   * データベースのタグコンテナとタグキャッシュに含まれているか確かめる
   *
   * @return
   */
  def hasTag(player: Player, tagId: String): Boolean = {
    plugin.database.getTags(uuid = player.getUniqueId.toString).contains(tagId) && cache.contains(tagId)
  }


  /**
   * タグをセットする
   */
  def setTag(player: Player, tagId: String): Unit = {
    // タグ情報をデータベース側に送るのはプレイヤーがログアウトしたときかセーブしたときのみ！
    val tag = cache.getOrElse(tagId, "-")
    plugin.database.setTag(player.getUniqueId.toString, tagId)
    val board = WarsCoreAPI.scoreboards(player)
    val obj = board.getObjective("tag")
    obj.setDisplayName(s"$tag §f0")
    WarsCoreAPI.scoreboards.values
      .map(_.getObjective("tag"))
      .foreach(_.getScore(player.getName).setScore(0))
  }


  /**
   * タグを追加する(= 新たに獲得)
   */
  def addTag(player: Player, tagId: String): Unit = {

  }

  /**
   * アイテムスタックのタグ表示名からタグキーを取得する。<br>
   * 少し頭がいい
   *
   * @return
   */
  def getTagKeyFromItemStack(item: ItemStack): String = {
    if (!item.hasItemMeta || !item.getItemMeta.hasLore) return ""
    """タグ名:.*;""".r.findFirstMatchIn(String.join(";", item.getItemMeta.getLore)) match {
      case Some(result) =>
        cache.find(_._2 == result.toString().replace(";", "")).map(_._1).getOrElse("NotFound")
      case None => ""
    }
  }
}
