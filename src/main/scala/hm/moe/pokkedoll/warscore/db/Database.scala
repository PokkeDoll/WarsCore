package hm.moe.pokkedoll.warscore.db

import java.util.UUID

import hm.moe.pokkedoll.warscore.games.TeamDeathMatch
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore}
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

import scala.collection.mutable

/**
 * データベースとのデータをやり取りするトレイト <br>
 *
 * version2.0にて、コールバックが追加された
 *
 * @author Emorard
 * @version 2.0
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
   * タグを取得する
   *
   * @param uuid     UUIDを指定
   * @param callback 非同期で返される
   * @version 2
   * @since v1.3
   */
  def getTags(uuid: String, callback: Callback[mutable.Buffer[(String, Boolean)]])


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
   * @param wp
   * @param col normalまたはgame
   */
  def getVInventory(wp: WPlayer, col: String = "normal")

  /**
   * 仮想インベントリをセーブする
   *
   * @param wp
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
   * アイテムを読み込む
   *
   * @since v1.2
   * @param uuid     対象のUUID
   * @param baseSlot ベースページ (page - 1) * 45 で求まる
   * @param callback | String Type
   *                 | Array[Byte] アイテムのRAWデータ
   *                 | Int slot
   *                 | Int use!?
   */
  def getPagedWeaponStorage(uuid: String, baseSlot: Int, callback: Callback[mutable.Buffer[(Int, Array[Byte], Int)]])

  /**
   * アイテムを保存する。
   *
   * @since v1.2
   * @param uuid     対象のUUID
   * @param baseSlot ベースページ (page - 1) * 45 で求まる
   * @param contents 保存するデータ
   */
  def setPagedWeaponStorage(uuid: String, baseSlot: Int, contents: Map[Boolean, Seq[(Int, ItemStack)]])

  /**
   * 武器を設定する
   * @since v1.3.4
   * @param uuid
   * @param slot
   */
  def setPagedWeapon(uuid: String, slot: Int, callback: Callback[Unit])

  /**
   * 現在使用している(use=1)の武器を読み込む
   * @param uuid
   * @param callback
   */
  def getWeapon(uuid: String, callback: Callback[mutable.Buffer[Array[Byte]]])

  def close(): Unit
}
