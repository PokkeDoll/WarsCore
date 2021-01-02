package hm.moe.pokkedoll.warscore.db

import hm.moe.pokkedoll.warscore.games.TeamDeathMatch
import hm.moe.pokkedoll.warscore.utils.TagUtil.UserTagInfo
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore}
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

import scala.collection.mutable

/**
 * データベースとのデータをやり取りするトレイト <br>
 *
 * version2.0にて、コールバックが追加された
 * version3.0では、リファクタリングが行われた
 *
 * @author Emorard
 * @version 3.0
 */
trait Database extends WeaponDB {
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
   * データベースから一つのInt型のデータを取得する
   *
   * @return
   */
  @Deprecated
  def getInt(table: String, column: String, uuid: String): Option[Int]

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
   * タグを取得する
   *
   * @param uuid     UUIDを指定
   * @param callback 非同期で返される
   * @version 2
   * @since v1.3
   */
  def getTags(uuid: String, callback: Callback[Vector[UserTagInfo]])

  /**
   * 設定しているタグを返す
   *
   * @param uuid     UUIDを指定
   * @param callback 非同期で返される
   * @version 2
   * @since v1.3
   */
  def getTag(uuid: String, callback: Callback[String])

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

  def gameLog(gameid: String, level: String, message: String)

  /**
   * 仮想インベントリを読み込む
   *
   * @param wp WPlayer
   * @param col normalまたはgame
   */
  def getVInventory(wp: WPlayer, col: String = "normal")

  /**
   * 仮想インベントリをセーブする
   *
   * @param wp WPlayer
   * @param col normalまたはgame
   */
  def setVInventory(wp: WPlayer, col: String = "normal")

  /**
   * WPプレイヤーの保存データを読み込む
   *
   * @version 2.0
   * @since v1.1.18
   * @param wp       対象のプレイヤー
   * @param callback 取得したデータを同期的に返す
   */
  def loadWPlayer(wp: WPlayer, callback: Callback[WPlayer])

  /**
   * 仮のインベントリ(ロビーのインベントリを取得する
   *
   * @version v1.3.15
   * @param uuid 対象のUUID
   * @param callback (スロット番号, シリアライズされたアイテムスタック)のタプル
   */
  def getVInv(uuid: String, callback: Callback[mutable.Buffer[(Int, Array[Byte])]])

  /**
   * ロビーのインベントリを退避する
   *
   * @version v1.3.15
   * @param uuid 対象のUUID
   * @param contents Inventory.getContent()
   */
  def setVInv(uuid: String, contents: Array[ItemStack], callback: Callback[Unit])

  /**
   * 試合中に切断した場合(Gameインスタンスが設定している場合)にtrueにする
   *
   * @version v1.3.22
   * @param uuid 対象のUUID
   */
  def setDisconnect(uuid: String, disconnect: Boolean)

  def close(): Unit
}
