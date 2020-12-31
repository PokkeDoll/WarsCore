package hm.moe.pokkedoll.warscore.db

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
   * @param data 武器のデータ
   */
  def setWeapon(uuid: String, t: String, data: String)

  /**
   * 武器を追加する
   *
   * @param uuid 対象のUUID
   * @param t    武器のタイプ
   * @param data 武器のデータ
   */
  def addWeapon(uuid: String, t: String, data: String)
}

object WeaponDB {
  val PRIMARY = "primary"
  val SECONDARY = "secondary"
  val MELEE = "melee"
  val ITEM = "item"

  def is(string: String): Boolean = Array(PRIMARY, SECONDARY, MELEE, ITEM).contains(string)
}
