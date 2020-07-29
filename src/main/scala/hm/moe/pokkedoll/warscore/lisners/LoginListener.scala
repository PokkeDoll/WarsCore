package hm.moe.pokkedoll.warscore.lisners

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.event.player.{PlayerJoinEvent, PlayerQuitEvent}
import org.bukkit.event.{EventHandler, Listener}

class LoginListener(plugin: WarsCore) extends Listener {
  @EventHandler
  def onJoin(e: PlayerJoinEvent): Unit = {
    WarsCoreAPI.getWPlayer(e.getPlayer)
  }

  @EventHandler
  def onQuit(e: PlayerQuitEvent): Unit = {
    val wp = WarsCoreAPI.getWPlayer(e.getPlayer)
    wp.game match {
      case Some(game) =>
        game.hub(wp)
      case _ =>
    }
  }
/*
  @EventHandler
  def onHandshake(e: PlayerHandshakeEvent): Unit = {
    //plugin.getLogger.info("e.getOriginalHandshake:" + e.getOriginalHandshake)
    //plugin.getLogger.info("e.getPropertiesJson: " + e.getPropertiesJson)
  }
 */
}
