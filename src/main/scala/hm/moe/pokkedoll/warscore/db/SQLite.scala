package hm.moe.pokkedoll.warscore.db
import java.util.UUID

import hm.moe.pokkedoll.warscore.WPlayer

class SQLite extends Database {
  /**
   * UUID(=データ、つまりテーブル)があるか確認するメソッド
   *
   * @param uuid
   * @return データがあるならtrueを返す
   */
  override def hasUUID(uuid: UUID): Boolean = ???

  /**
   * テーブルからデータを読み込むメソッド
   *
   * @param wp
   * @return 読み込みエラーが発生したらNone
   */
  override def loadWPlayer(wp: WPlayer): Option[WPlayer] = ???

  /**
   * テーブルにデータを保存するメソッド
   */
override def saveWPlayer(wp: WPlayer): Option[WPlayer] = ???

  /**
   * データを登録するメソッド
   *
   * @param uuid
   * @return
   */
override def insert(uuid: UUID): Boolean = ???
}
