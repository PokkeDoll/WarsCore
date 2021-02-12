package hm.moe.pokkedoll.warscore.db

import org.bukkit.inventory.ItemStack

import scala.util.Try

/**
 * item.ymlによる管理だったが、データベースに移行したほうが楽だと感じたので
 *
 * @author Emorard
 */
trait ItemDB {
  /**
   * item.ymlからデータベースへ移動する
   */
  def migrate(): Try[Unit]

  /**
   * データベースにアイテムを登録する
   * @param name アイテムのID
   * @param item アイテム, Noneなら削除を意味する
   */
  def update(name: String, item: Option[ItemStack]): Try[Unit]

  /**
   * データベースのカラムをすべて持ってくる
   * @return
   */
  def getItems(): Try[Seq[(String, String, ItemStack)]]
}
