package hm.moe.pokkedoll.warscore.db

import java.util.UUID

import hm.moe.pokkedoll.warscore.games.TeamDeathMatch
import hm.moe.pokkedoll.warscore.ui.WeaponUI
import hm.moe.pokkedoll.warscore.utils.TagUtil.UserTagInfo
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore}
import org.bukkit.entity.Player
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
trait Database {
  /**
   * @see hasUUID(uuid: String): Boolean
   */
  @Deprecated
  def hasUUID(uuid: UUID): Boolean = hasUUID(uuid.toString)

  /**
   * @see hasUUID(uuid: String): Boolean
   */
  @Deprecated
  def hasUUID(player: Player): Boolean = hasUUID(player.getUniqueId.toString)

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
   * @see insert(uuid: UUID): Boolean
   */
  @Deprecated
  def insert(player: Player): Boolean = insert(player.getUniqueId.toString)

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
  @Deprecated
  def getStorage(id: Int, uuid: String): Option[String]

  /**
   * ストレージを保存する
   *
   * @param id   エンダーチェストのID
   * @param uuid UUID
   * @param item アイテムの文字列
   */
  @Deprecated
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
   * すべてのアイテムを読み込む！！非同期メソッドで利用
   * @param uuid UUID
   */
  def getWeaponStorage(uuid: String): Vector[ItemStack]

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
   *
   * @since v1.3.4
   * @param uuid     対象のUUID
   * @param slot     新しく設定するスロット
   * @param usedSlot 以前設定していた純粋なスロット(ベースページとかインベントリ上段の処理を考える必要がない)
   */
  def setPagedWeapon(uuid: String, slot: Int, usedSlot: Int, usedType: Int, callback: Callback[Unit])

  /**
   * 現在使用している(use>0)の武器を読み込む
   *
   * @param uuid
   * @param callback (アイテムのバイトデータ, 使用タイプ)
   */
  def getWeapon(uuid: String, callback: Callback[mutable.Buffer[(Array[Byte], Int)]])

  /**
   * 仮のインベントリ(ロビーのインベントリを取得する
   *
   * @version v1.3.15
   * @param uuid
   * @param callback (スロット番号, シリアライズされたアイテムスタック)のタプル
   */
  def getVInv(uuid: String, callback: Callback[mutable.Buffer[(Int, Array[Byte])]])

  /**
   * ロビーのインベントリを退避する
   *
   * @version v1.3.15
   * @param uuid
   * @param contents
   */
  def setVInv(uuid: String, contents: Array[ItemStack], callback: Callback[Unit])

  /**
   * 試合中に切断した場合(Gameインスタンスが設定している場合)にtrueにする
   *
   * @version v1.3.22
   * @param uuid 対象のUUID
   */
  def setDisconnect(uuid: String, disconnect: Boolean)

  /**
   * アクティブなマイセットを獲得する
   * @param uuid 対象のUUID
   * @param callback コールバック
   */
  def getActiveMySet(uuid: String, callback: Callback[Array[Array[Byte]]])

  /**
   *
   */
  def checkMySet(uuid: String, slot: Int): Boolean

  /**
   * マイセットを読み込む。非同期メソッドで使う！！
   *
   * @since v1.4.3
   * @param uuid     対象のUUID
   * @return
   */
  def getMySet(uuid: String): Vector[WeaponUI.MySet]

  /**
   * マイセットを設定する
   *
   * @since v1.4.3
   * @param uuid
   * @param slot
   * @param callback
   */
  def setMySet(uuid: String, slot: Int, callback: Callback[Unit])

  /**
   * マイセットを適用する
   * @since v1.4.18
   */
  def applyMySet(uuid: String, slot: Int, callback: Callback[Unit])

  def deleteMySet(uuid: String, slot: Int)

  /**
   * アイテムを自動でストレージに保存する
   * @param uuid
   * @param array
   */
  def addItem(uuid: String, array: Array[Byte]*)

  def close(): Unit
}
