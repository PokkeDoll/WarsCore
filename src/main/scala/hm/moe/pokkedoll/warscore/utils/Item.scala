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

object Item {
  def of(string: String): Option[Item] = {
    val split = string.split("@")
    if (split.length == 2) {
      Some(new Item(split(0), try {
        split(1).toInt
      } catch {
        case _: NumberFormatException => 0
      }))
    } else {
      None
    }
  }
}
