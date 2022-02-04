/*
package hm.moe.pokkedoll.warscore.utils
import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.scheduler.BukkitRunnable

private[utils] class CustomRunnable {

  private var _delay = 0L

  def runTask(task: Unit): CustomRunnable = {
    val runnable: BukkitRunnable = {
      () => task
    }
    runnable.runTaskLater(WarsCore.getInstance, _delay)
    this
  }

  def delay(_delay: Long): CustomRunnable = {
    this._delay += _delay
    this
  }
}

object CustomRunnable {
  def create: CustomRunnable = new CustomRunnable()
}
 */