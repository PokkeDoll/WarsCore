package hm.moe.pokkedoll.warscore.db

import java.util.UUID

import hm.moe.pokkedoll.warscore.WPlayer
import org.bukkit.entity.Player

/**
 *  データベースとのデータをやり取りするトレイト
 *  @author Emorard
 */
trait Database {
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
}
