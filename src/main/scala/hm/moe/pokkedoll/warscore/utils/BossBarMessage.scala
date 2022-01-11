package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.Bukkit
import org.bukkit.boss.{BarColor, BarStyle}
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

object BossBarMessage {

  private val _padding = " " * 20

  def send(message: String, seconds: Long, players: Player*): Unit = {
    val b = Bukkit.createBossBar(_padding + message, BarColor.WHITE, BarStyle.SOLID)
    b.setVisible(true)
    if(players.isEmpty)
      Bukkit.getOnlinePlayers.forEach(player => b.addPlayer(player))
    else
      players.foreach(player => b.addPlayer(player))
    new BukkitRunnable {
      override def run(): Unit = {
        b.setVisible(false)
        b.removeAll()
      }
    }.runTaskLater(WarsCore.instance, seconds * 20)
  }
}
