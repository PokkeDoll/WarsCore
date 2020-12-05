package hm.moe.pokkedoll.warscore.utils

import java.io.File

import hm.moe.pokkedoll.warscore.{Test, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.entity.Player

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

object TagUtil {
  private lazy val plugin = WarsCore.instance

  private lazy val db = WarsCore.instance.database

  var configFile: File = _
  var config: FileConfiguration = _

  /**
   * コンフィグから取得される、有効なタグのキャッシュ
   */
  var cache: Map[String, TagInfo] = _

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
    cache = cs.asScala.map(f => (f, new TagInfo(f, ChatColor.translateAlternateColorCodes('&', config.getString(s"$f.name", "null")), config.getInt(s"$f.price", -1)))).toMap
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

  def getTag(tagId: String): TagInfo = {
    cache.getOrElse(tagId, new TagInfo("", "", -1))
  }

  def isLimited(tagInfo: TagInfo): Boolean = tagInfo.price == -1 || tagInfo.rank == -1

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

  class TagInfo(val id: String, val name: String, val price: Int = -1, val rank: Int = -1)
}
