package hm.moe.pokkedoll.warscore.db

import hm.moe.pokkedoll.warscore.utils.Item
import org.jetbrains.annotations.Nullable

import java.sql.SQLException
import scala.util.Try

trait WeaponDB {
  /**
   * データベースから未加工のデータを取得する
   *
   * @param uuid   UUID
   * @param offset 取得を始める番号
   * @return (type, name, amount, use)の組
   */
  def getOriginalItem(uuid: String, offset: Int): List[WeaponDB.OriginalItemSet]

  /**
   * 旧getOriginalItemの互換性対応版
   * @param uuid
   * @param offset
   * @return
   */
  @Deprecated
  def getOriginalItemLegacy(uuid: String, offset: Int): List[(String, String, Int, Boolean)]

  /**
   * データベースから未加工のデータを取得する
   *
   * @param uuid       UUID
   * @param offset     取得を始める番号
   * @param weaponType アイテムのタイプ
   * @return (name, amount, use)の組
   */
  def getOriginalItem(uuid: String, offset: Int, weaponType: String): List[WeaponDB.OriginalItemSet]

  /**
   * 旧getOriginalItemの互換性版
   * @param uuid
   * @param offset
   * @param weaponType
   * @return
   */
  @Deprecated
  def getOriginalItemLegacy(uuid: String, offset: Int, weaponType: String): List[(String, Int, Boolean)]

  /**
   * データベースからアイテムの数字を取得する
   *
   * @param uuid       UUID
   * @param name       名前
   * @param weaponType タイプ
   * @return
   */
  def getAmount(uuid: String, name: String, weaponType: String = "item"): Int

  /**
   * 武器を取得する
   *
   * @param uuid       対象のUUID
   * @param weaponType 武器のタイプ
   * @param sortType   ソートタイプ
   * @return 武器たち
   */
  def getWeapons(uuid: String, weaponType: String, sortType: Int = 0): Seq[Item]

  def getWeapons4J(uuid: String, weaponType: String, sortType: Int = 0): java.util.List[Item]

  /**
   * 現在設定されている武器のリストを取得する
   *
   * @param uuid 対象のUUID
   * @return 武器のタプル(メイン, サブ, 近接, アイテム)
   */
  def getActiveWeapon(uuid: String): WeaponDB.ActiveWeaponSet

  /**
   * 武器をセットする
   *
   * @param uuid       対象のUUID
   * @param weaponType 武器のタイプ
   * @param name       武器のデータ
   */
  def setWeapon(uuid: String, weaponType: String, name: String)

  /**
   * 武器を追加する
   *
   * @param uuid       対象のUUID
   * @param weaponType 武器のタイプ
   * @param name       武器のデータ
   */
  def addWeapon(uuid: String, weaponType: String, name: String, amount: Int): Try[Unit]

  def addWeapon4J(uuid: String, weaponType: String, name: String, amount: Int): Boolean

  /**
   * アイテムを追加する。タイプはitemに固定される。さらに非同期！
   *
   * @param uuid 対象のUUID
   * @param item アイテム
   */
  def addItem(uuid: String, item: Array[Item])

  /**
   * 武器を削除する
   *
   * @param uuid  対象のUUID
   * @param price 価格
   */
  def delWeapon(uuid: String, price: Array[Item])

  /**
   * 実際にプレイやーが所持しているアイテムを付け加えて返す。非同期で使う
   *
   * @param uuid UUID
   * @param item Shop.priceで獲得できる
   * @return Map
   */
  def getRequireItemsAmount(uuid: String, item: Array[Item]): Map[String, Int]
}

object WeaponDB {
  val PRIMARY = "primary"
  val SECONDARY = "secondary"
  val MELEE = "melee"
  val GRENADE = "grenade"
  val ITEM = "item"
  val HEAD = "head"

  def is(string: String): Boolean = Array(PRIMARY, SECONDARY, MELEE, GRENADE, ITEM, HEAD).contains(string)

  /**
   * データベースから取得するアクティブ武器のセット(タプルの代わり)
   */
  class ActiveWeaponSet(val main: String, val sub: String, val melee: String, val item: String, val head: String)

  class OriginalItemSet(@Nullable val `type`: String, val name: String, val amount: Int, val use: Boolean)
}
