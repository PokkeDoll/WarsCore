package hm.moe.pokkedoll.warscore.db

@Deprecated
trait CoinDB {
  /**
   * すべてのコインを取得する
   *
   * @param uuid     対象のUUID
   * @param t        行のタイプ
   */
  def getCoin(uuid: String, t: Array[String]): Map[String, Int]

  /**
   * 金だけを取得する
   *
   * @param uuid     対象のUUID
   */
  def getMoney(uuid: String): Int
}

object CoinDB {
  // ぽっけコイン
  val PC = "pc"

}
