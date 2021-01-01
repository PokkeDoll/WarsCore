package hm.moe.pokkedoll.warscore.db

import hm.moe.pokkedoll.warscore.Callback
import hm.moe.pokkedoll.warscore.utils.Item
import hm.moe.pokkedoll.warscore.utils.ShopUtil.Shop

trait WeaponDB {
  /**
   * 武器を取得する
   *
   * @param uuid 対象のUUID
   * @param t    武器のタイプ
   * @return 武器たち
   */
  def getWeapons(uuid: String, t: String): Seq[String]

  /**
   * 現在設定されている武器のリストを取得する
   *
   * @param uuid 対象のUUID
   * @return 武器のタプル(メイン, サブ, 近接, アイテム)
   */
  def getActiveWeapon(uuid: String): (String, String, String, String)

  /**
   * 武器をセットする
   *
   * @param uuid 対象のUUID
   * @param t    武器のタイプ
   * @param name 武器のデータ
   */
  def setWeapon(uuid: String, t: String, name: String)

  /**
   * 武器を追加する
   *
   * @param uuid 対象のUUID
   * @param t    武器のタイプ
   * @param name 武器のデータ
   */
  def addWeapon(uuid: String, t: String, name: String, amount: Int)

  /**
   * 武器を削除する
   * @param uuid
   * @param price
   */
  def delWeapon(uuid: String, price: Array[Item])

  /**
   * 武器を購入する
   * @param uuid 対象のUUID
   * @param shop 購入しようとするショップの商品
   * @return エラーがあるならSomeが返される！！！！！！！！１１
   */
  def buyWeapon(uuid: String, shop: Shop): Option[String]

  /**
   * 武器を購入できるか確かめる
   * @param uuid
   * @param shop
   * @return
   */
  def isBuyable(uuid: String, shop: Shop): Seq[String]

  /**
   * 実際にプレイやーが所持しているアイテムを付け加えて返す。非同期で使う
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
  val ITEM = "item"

  def is(string: String): Boolean = Array(PRIMARY, SECONDARY, MELEE, ITEM).contains(string)
}
