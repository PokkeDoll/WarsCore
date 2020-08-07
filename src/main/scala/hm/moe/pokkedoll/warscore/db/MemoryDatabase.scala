package hm.moe.pokkedoll.warscore.db

import java.util.UUID

import hm.moe.pokkedoll.warscore.WPlayer
import org.bukkit.Bukkit

import scala.collection.mutable

/**
 * デバッグ用HashMapを用いたリレーショナルデータベース <br>
 * 当然ながらアンロードするとデータは消滅する
 *
 * @author Emorard
 */
class MemoryDatabase extends Database {
  val player = mutable.HashMap.empty[UUID, WPlayer]

  /**
   * UUID(=データ、つまりテーブル)があるか確認するメソッド
   *
   * @param uuid
   * @return データがあるならtrueを返す
   */
  override def hasUUID(uuid: UUID): Boolean = player.contains(uuid)

  /**
   * テーブルからデータを読み込むメソッド
   *
   * @param wp
   * @return 読み込みエラーが発生したらNone
   */
  override def loadWPlayer(wp: WPlayer): Option[WPlayer] = {
    // TODO 読み込み処理
    Some(wp)
  }

  /**
   * テーブルにデータを保存するメソッド
   */
  override def saveWPlayer(wp: WPlayer): Option[WPlayer] = {
    player.put(wp.player.getUniqueId, wp)
  }

  /**
   * データを登録するメソッド
   *
   * @param uuid
   * @return
   */
  override def insert(uuid: UUID): Boolean = {
    player.put(uuid, new WPlayer(Bukkit.getPlayer(uuid))).isDefined
  }

  /**
   * インスタンス作成時に呼び出されるメソッド
   */
  override def init(): Unit = ???

  /**
   * データベースから一つのInt型のデータを取得する
   *
   * @return
   */
  override def getInt(table: String, column: String, uuid: String): Option[Int] = ???

  /**
   * ストレージを取得する<br>
   *
   * @note データベースが返すのはItemStack型ではない。Array[Byte]->String->YamlConfiguration->Array[ItemStack]の手順が必要
   * @param id
   * @param uuid
   * @return
   */
  override def getStorage(id: Int, uuid:  String): Option[Array[Byte]] = ???
}
