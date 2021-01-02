package hm.moe.pokkedoll.warscore

import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class PeriodicMessage(val messages: List[String]) extends BukkitRunnable {
  override def run(): Unit = {
    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', scala.util.Random.shuffle(messages).head))
  }
}
