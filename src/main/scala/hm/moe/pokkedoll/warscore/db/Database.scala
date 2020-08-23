package hm.moe.pokkedoll.warscore.db

import java.rmi.server.UnicastRemoteObject
import java.util.UUID

import hm.moe.pokkedoll.warscore.WPlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 *  データベースとのデータをやり取りするトレイト
 *  @author Emorard
 */
trait Database {
  /**
   * インスタンス作成時に呼び出されるメソッド
   */
  def init(): Unit

  /**
   * UUID(=データ、つまりテーブル)があるか確認するメソッド
   * @param uuid
   * @return データがあるならtrueを返す
   */
  def hasUUID(uuid: UUID): Boolean

  /**
   * @see hasUUID(uuid: UUID): Boolean
   */
  def hasUUID(player: Player): Boolean = hasUUID(player.getUniqueId)

  /**
   * テーブルからデータを読み込むメソッド <br>
   * 0.17より方針上の都合のため非推奨化
   * @param wp
   * @return 必ずNone
   */
  @Deprecated
  def loadWPlayer(wp: WPlayer): Option[WPlayer]

  /**
   * テーブルにデータを保存するメソッド
   */
  def saveWPlayer(wp: WPlayer): Option[WPlayer]

  /**
   * データを登録するメソッド
   * @param uuid
   * @return
   */
  def insert(uuid: UUID): Boolean

  /**
   * @see insert(uuid: UUID): Boolean
   */
  def insert(player: Player): Boolean = insert(player.getUniqueId)

  /**
   * データベースから一つのInt型のデータを取得する
   * @return
   */
  def getInt(table: String, column: String, uuid: String): Option[Int]

  /**
   * ストレージを取得する<br>
   * @note データベースが返すのはItemStack型ではない。Array[Byte]->String->YamlConfiguration->Array[ItemStack]の手順が必要
   * @param id
   * @param uuid
   * @return
   */
  def getStorage(id: Int, uuid: String): Option[String]

  /**
   * ストレージを保存する
   * @param id
   * @param uuid
   * @param item
   */
  def setStorage(id: Int, uuid: String, item: String): Unit

  /**
   * rankテーブルに保存されているデータを取得する
   * @param uuid
   * @return
   */
  def getRankData(uuid: String): Option[(Int, Int)]

  def updateTDM(): Boolean

  def close(): Unit
}
