package hm.moe.pokkedoll.warscore

import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class PeriodicMessage(private val messages: List[String]) extends BukkitRunnable {

  private var stack: Seq[String] = Seq.empty[String]

  def getMessage: String = {
    if(stack.isEmpty) stack = scala.util.Random.shuffle(messages)
    val msg = stack.head
    stack = stack.tail
    msg
  }

  override def run(): Unit = {
    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', getMessage))
  }
}
