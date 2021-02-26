package hm.moe.pokkedoll.warscore.db

import scala.util.Try

/**
 * タグに関するメソッドを持つトレイト
 */
trait TagDB {
  /**
   * すべてのタグを取得する
   * @return
   */
  def getTags: Try[Seq[(String, String)]]
}
