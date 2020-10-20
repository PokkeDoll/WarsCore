package hm.moe.pokkedoll.warscore.lisners

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.event.player.{PlayerJoinEvent, PlayerQuitEvent}
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, ChatColor}

class LoginListener(plugin: WarsCore) extends Listener {
  @EventHandler
  def onJoin(e: PlayerJoinEvent): Unit = {
    val player = e.getPlayer

    e.setJoinMessage(ChatColor.GREEN + s"Connect: ${player.getName}")

    if(!plugin.database.hasUUID(player)) {
      plugin.database.insert(player)
    }

    WarsCoreAPI.getWPlayer(player)
    //リソースパックのデータ送信を行う
    player.sendMessage("§9クライアントのバージョンを取得しています...")
    new BukkitRunnable {
      override def run(): Unit = {
        if(!player.hasPlayedBefore) {
          player.teleport(WarsCoreAPI.FIRST_SPAWN)
        } else if(player.getWorld.getName!="p-lobby") {
          player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
        }
        val out = ByteStreams.newDataOutput
        out.writeUTF("PlayerVersion")
        player.sendPluginMessage(plugin, "pokkedoll:torus", out.toByteArray)

        player.sendMessage(WarsCoreAPI.NEWS: _*)

        WarsCoreAPI.addScoreBoard(player)
      }
    }.runTaskLater(plugin, 5L)
  }

  @EventHandler
  def onQuit(e: PlayerQuitEvent): Unit = {
    e.setQuitMessage("")
    val player = e.getPlayer
    if(player.getWorld.getName!="p-lobby") {
      player.teleport(Bukkit.getWorlds.get(0).getSpawnLocation)
    }
    WarsCoreAPI.removeScoreboard(player)
    val wp = WarsCoreAPI.getWPlayer(player)
    wp.game match {
      case Some(game) =>
        game.hub(wp)
      case _ =>
    }
    // キャッシュから削除
    WarsCoreAPI.wplayers.remove(player)
  }
}
