package hm.moe.pokkedoll.warscore.utils

/**
 * ItemUtilで使うデータ化されたアイテム(の名前)と数
 *
 * @param name   名前
 * @param amount 数
 */
class Item(val name: String, val amount: Int) {
  override def equals(obj: Any): Boolean = {
    obj match {
      case item: Item =>
        this.name == item.name && this.amount == item.amount
      case _ => false
    }
  }
}
