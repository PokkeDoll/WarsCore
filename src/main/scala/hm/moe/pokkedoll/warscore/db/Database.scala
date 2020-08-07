package hm.moe.pokkedoll.warscore.db

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
   * テーブルからデータを読み込むメソッド
   * @param wp
   * @return 読み込みエラーが発生したらNone
   */
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
  def getStorage(id: Int, uuid: String): Option[Array[Byte]]
}
