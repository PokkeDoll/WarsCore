package hm.moe.pokkedoll.warscore.lisners

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.{Callback, WPlayer, WarsCore, WarsCoreAPI}
import org.bukkit.event.player.{PlayerJoinEvent, PlayerQuitEvent}
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, ChatColor}

class LoginListener(plugin: WarsCore) extends Listener {
  @EventHandler
  def onJoin(e: PlayerJoinEvent): Unit = {
    val player = e.getPlayer

    e.setJoinMessage(ChatColor.GREEN + s"Connect: ${player.getName}")

    if (!plugin.database.hasUUID(player)) {
      plugin.database.insert(player)
    }


    WarsCore.instance.database.loadWPlayer(WarsCoreAPI.getWPlayer(player), new Callback[WPlayer] {
      override def success(value: WPlayer): Unit = {

      }

      override def failure(error: Exception): Unit = {
        error.printStackTrace()
        player.sendMessage(ChatColor.RED + "データの読み込みに失敗しました")
      }
    })
    //リソースパックのデータ送信を行う
    player.sendMessage("§9クライアントのバージョンを取得しています...")
    new BukkitRunnable {
      override def run(): Unit = {
        if (!player.hasPlayedBefore) {
          player.teleport(WarsCoreAPI.FIRST_SPAWN)
        } else if (player.getWorld.getName != "p-lobby") {
          player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
        }
        // リソースパックの送信
        val out = ByteStreams.newDataOutput
        out.writeUTF("ResourcePack")
        out.writeUTF("Wars")
        player.sendPluginMessage(WarsCore.instance, WarsCore.MODERN_TORUS_CHANNEL, out.toByteArray)

        player.sendMessage(WarsCoreAPI.NEWS: _*)

        WarsCoreAPI.addScoreBoard(player)
      }
    }.runTaskLater(plugin, 5L)
  }

  @EventHandler
  def onQuit(e: PlayerQuitEvent): Unit = {
    e.setQuitMessage("")
    val player = e.getPlayer
    if (player.getWorld.getName != "p-lobby") {
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
