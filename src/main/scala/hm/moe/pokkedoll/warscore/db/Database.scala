package hm.moe.pokkedoll.warscore.db

import java.util.UUID

import hm.moe.pokkedoll.warscore.games.TeamDeathMatch
import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore}
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

/**
 * データベースとのデータをやり取りするトレイト
 *
 * @author Emorard
 */
trait Database {
  /**
   * インスタンス作成時に呼び出されるメソッド
   */
  def init(): Unit

  /**
   * UUID(=データ、つまりテーブル)があるか確認するメソッド
   *
   * @param uuid UUID
   * @return データがあるならtrueを返す
   */
  def hasUUID(uuid: UUID): Boolean

  /**
   * @see hasUUID(uuid: UUID): Boolean
   */
  def hasUUID(player: Player): Boolean = hasUUID(player.getUniqueId)

  /**
   * テーブルにデータを保存するメソッド
   */
  def saveWPlayer(wp: WPlayer): Option[WPlayer]

  /**
   * データを登録するメソッド
   *
   * @param uuid UUID
   * @return
   */
  def insert(uuid: UUID): Boolean

  /**
   * @see insert(uuid: UUID): Boolean
   */
  def insert(player: Player): Boolean = insert(player.getUniqueId)

  /**
   * データベースから一つのInt型のデータを取得する
   *
   * @return
   */
  def getInt(table: String, column: String, uuid: String): Option[Int]

  /**
   * ストレージを取得する<br>
   *
   * @note データベースが返すのはItemStack型ではない。Array[Byte]->String->YamlConfiguration->Array[ItemStack]の手順が必要
   * @param id   エンダーチェストのID
   * @param uuid UUID
   * @return
   */
  def getStorage(id: Int, uuid: String): Option[String]

  /**
   * ストレージを保存する
   *
   * @param id   エンダーチェストのID
   * @param uuid UUID
   * @param item アイテムの文字列
   */
  def setStorage(id: Int, uuid: String, item: String): Unit

  /**
   * rankテーブルに保存されているデータを取得する
   *
   * @param uuid UUID
   * @return
   */
  def getRankData(uuid: String): Option[(Int, Int)]

  def setRankData(uuid: String, data: (Int, Int))

  /**
   * TDMの戦績を更新する
   *
   * @param game TDM
   * @return
   */
  def updateTDM(game: TeamDeathMatch): Boolean

  def updateTDMAsync(game: TeamDeathMatch): Unit = {
    new BukkitRunnable {
      override def run(): Unit = updateTDM(game)
    }.runTaskAsynchronously(WarsCore.instance)
  }

  /**
   * 現在設定しているタグを獲得する テーブル tagより
   *
   * @param uuid UUID
   * @return
   */
  def getTag(uuid: String): String

  /**
   * 所持しているタグを獲得する テーブル tagContainerより
   *
   * @param uuid UUID
   * @return
   */
  def getTags(uuid: String): IndexedSeq[String]

  /**
   * タグをセットする
   *
   * @param uuid UUID
   * @param id   タグID
   */
  def setTag(uuid: String, id: String)

  /**
   * タグコンテナにタグを追加する
   *
   * @param uuid UUID
   * @param id   タグID
   */
  def addTag(uuid: String, id: String)

  def close(): Unit
}
