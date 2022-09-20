package hm.moe.pokkedoll.warscore.db

import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore}
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

import scala.collection.mutable
import scala.util.Try

/**
 * データベースとのデータをやり取りするトレイト <br>
 *
 * version2.0にて、コールバックが追加された
 * version3.0では、リファクタリングが行われた
 *
 * @author Emorard
 * @version 3.0
 */
trait Database extends WeaponDB with ItemDB {
  /**
   * データベースに自分のデータがあるか確認するメソッド
   *
   * @since v1.4.1
   * @param uuid 対象のUUID
   * @return UUIDが存在すればtrue
   */
  def hasUUID(uuid: String): Boolean

  /**
   * データを登録するメソッド
   *
   * @param uuid UUID
   * @return
   */
  def insert(uuid: String): Boolean

  /**
   * WPプレイヤーの保存データを読み込む
   *
   * @version 2.0
   * @since v1.1.18
   * @param wp       対象のプレイヤー
   * @param callback 取得したデータを同期的に返す
   */
  def loadWPlayer(wp: WPlayer, callback: Callback[WPlayer]): Unit

  /**
   * 試合中に切断した場合(Gameインスタンスが設定している場合)にtrueにする
   *
   * @version v1.3.22
   * @param uuid 対象のUUID
   */
  def setDisconnect(uuid: String, disconnect: Boolean): Unit

  def close(): Unit
}
