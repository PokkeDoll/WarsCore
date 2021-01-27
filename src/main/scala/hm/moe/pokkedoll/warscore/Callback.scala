package hm.moe.pokkedoll.warscore

trait Callback[T] {
  def success(value: T)
  def failure(error: Exception)

  /**
   * 注意！ デバッグ用！
   */
  var async: Boolean = false
}
