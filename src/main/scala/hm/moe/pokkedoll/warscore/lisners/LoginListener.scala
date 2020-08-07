package hm.moe.pokkedoll.warscore.lisners

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.ChatColor
import org.bukkit.event.player.{PlayerJoinEvent, PlayerQuitEvent}
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.scheduler.BukkitRunnable

class LoginListener(plugin: WarsCore) extends Listener {
  @EventHandler
  def onJoin(e: PlayerJoinEvent): Unit = {
    e.setJoinMessage(ChatColor.GREEN + s"Connect: ${e.getPlayer.getName}")
    WarsCoreAPI.getWPlayer(e.getPlayer)
    //リソースパックのデータ送信を行う
    e.getPlayer.sendMessage("§9クライアントのバージョンを取得しています...")
    new BukkitRunnable {
      override def run(): Unit = {
        val out = ByteStreams.newDataOutput
        out.writeUTF("PlayerVersion")
        e.getPlayer.sendPluginMessage(plugin, "pokkedoll:torus", out.toByteArray)

        e.getPlayer.sendMessage(WarsCoreAPI.NEWS: _*)

        WarsCoreAPI.addScoreBoard(e.getPlayer)
      }
    }.runTaskLater(plugin, 20L)
  }

  @EventHandler
  def onQuit(e: PlayerQuitEvent): Unit = {
    e.setQuitMessage("")
    WarsCoreAPI.removeScoreboard(e.getPlayer)
    val wp = WarsCoreAPI.getWPlayer(e.getPlayer)
    wp.game match {
      case Some(game) =>
        game.hub(wp)
      case _ =>
    }
  }
}
