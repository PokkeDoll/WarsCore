package hm.moe.pokkedoll.warscore.utils

import java.io.File
import hm.moe.pokkedoll.warscore.{Test, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.entity.Player

import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

/**
 * 一時期死んだTagUtilだが、満を持して復活！
 *
 * @author Emorard
 */
object TagUtil {
  private lazy val plugin = WarsCore.instance

  private lazy val db = WarsCore.instance.database

  /**
   * コンフィグから取得される、有効なタグのキャッシュ
   */
  //var cache: Map[String, TagInfo] = _

  var cache = Map.empty[String, String]

  def init(): Unit = {
    plugin.database.getTags match {
      case Success(value) =>
        cache = value.toMap
        plugin.info("Tagテーブルの読み込みに成功しました")
      case Failure(exception) =>
        exception.printStackTrace()
        plugin.info("Tagテーブルの読み込みに失敗しました")
    }
  }

  def getTag(tagId: String): String = {
    cache.getOrElse(tagId, "")
  }

  /**
   * タグをセットする
   */
  def setTag(player: Player, tagId: String): Unit = {
    /*
    // タグ情報をデータベース側に送るのはプレイヤーがログアウトしたときかセーブしたときのみ！
    val tag = cache.getOrElse(tagId, "-")
    plugin.database.setTag(player.getUniqueId.toString, tagId)
    val board = WarsCoreAPI.scoreboards(player)
    val obj = board.getObjective("tag")
    obj.setDisplayName(s"$tag §f0")
    WarsCoreAPI.scoreboards.values
      .map(_.getObjective("tag"))
      .foreach(_.getScore(player.getName).setScore(0))
    */
  }


}
