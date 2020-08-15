package hm.moe.pokkedoll.warscore.lisners

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.utils.RankManager
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.event.player.{PlayerJoinEvent, PlayerQuitEvent}
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.scheduler.BukkitRunnable

class LoginListener(plugin: WarsCore) extends Listener {
  @EventHandler
  def onJoin(e: PlayerJoinEvent): Unit = {
    lazy val player = e.getPlayer

    e.setJoinMessage(ChatColor.GREEN + s"Connect: ${player.getName}")

    if(!plugin.database.hasUUID(player)) {
      plugin.database.insert(player)
    }

    WarsCoreAPI.getWPlayer(player)
    //リソースパックのデータ送信を行う
    player.sendMessage("§9クライアントのバージョンを取得しています...")
    new BukkitRunnable {
      override def run(): Unit = {
        if(player.getWorld.getName!="p-lobby") {
          player.teleport(Bukkit.getWorlds.get(0).getSpawnLocation)
        }
        val out = ByteStreams.newDataOutput
        out.writeUTF("PlayerVersion")
        player.sendPluginMessage(plugin, "pokkedoll:torus", out.toByteArray)

        player.sendMessage(WarsCoreAPI.NEWS: _*)

        WarsCoreAPI.addScoreBoard(player)

        RankManager.updateScoreboard(player)
      }
    }.runTaskLater(plugin, 5L)
  }

  @EventHandler
  def onQuit(e: PlayerQuitEvent): Unit = {
    e.setQuitMessage("")
    if(e.getPlayer.getWorld.getName!="p-lobby") {
      e.getPlayer.teleport(Bukkit.getWorlds.get(0).getSpawnLocation)
    }
    WarsCoreAPI.removeScoreboard(e.getPlayer)
    val wp = WarsCoreAPI.getWPlayer(e.getPlayer)
    wp.game match {
      case Some(game) =>
        game.hub(wp)
      case _ =>
    }
  }
}
