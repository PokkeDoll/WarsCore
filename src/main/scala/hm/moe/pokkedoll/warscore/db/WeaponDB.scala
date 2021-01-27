package hm.moe.pokkedoll.warscore.db

import hm.moe.pokkedoll.warscore.utils.Item

trait WeaponDB {
  /**
   * データベースから未加工のデータを取得する
   *
   * @param uuid   UUID
   * @param offset 取得を始める番号
   * @return (type, name, amount, use)の組
   */
  def getOriginalItem(uuid: String, offset: Int): List[(String, String, Int, Boolean)]

  /**
   * データベースから未加工のデータを取得する
   *
   * @param uuid   UUID
   * @param offset 取得を始める番号
   * @param `type` アイテムのタイプ
   * @return (name, amount, use)の組
   */
  def getOriginalItem(uuid: String, offset: Int, `type`: String): List[(String, Int, Boolean)]

  /**
   * データベースからアイテムの数字を取得する
   *
   * @param uuid   UUID
   * @param name   名前
   * @param `type` タイプ
   * @return
   */
  def getAmount(uuid: String, name: String, `type`: String = "item"): Int

  /**
   * 武器を取得する
   *
   * @param uuid 対象のUUID
   * @param t    武器のタイプ
   * @return 武器たち
   */
  def getWeapons(uuid: String, t: String): Seq[Item]

  /**
   * 現在設定されている武器のリストを取得する
   *
   * @param uuid 対象のUUID
   * @return 武器のタプル(メイン, サブ, 近接, アイテム)
   */
  def getActiveWeapon(uuid: String): (String, String, String, String, String)

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
   * アイテムを追加する。タイプはitemに固定される。さらに非同期！
   *
   * @param uuid 対象のUUID
   * @param item アイテム
   */
  def addItem(uuid: String, item: Item*)

  /**
   * 武器を削除する
   *
   * @param uuid
   * @param price
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
}
