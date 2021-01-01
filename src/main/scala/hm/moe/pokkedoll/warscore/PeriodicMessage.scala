package hm.moe.pokkedoll.warscore

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class PeriodicMessage(val messages: List[String]) extends BukkitRunnable {
  override def run(): Unit = {
    Bukkit.broadcastMessage(scala.util.Random.shuffle(messages).head)
  }
}
