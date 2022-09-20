package hm.moe.pokkedoll.warscore

trait Callback[T] {
  def success(value: T): Unit
  def failure(error: Exception): Unit

  /**
   * 注意！ デバッグ用！
   */
  var async: Boolean = false
}
