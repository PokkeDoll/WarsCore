package hm.moe.pokkedoll.warscore.lisners

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.{EventHandler, Listener}

class PlayerListener(plugin: WarsCore) extends Listener {
  @EventHandler
  def onDeath(e: PlayerDeathEvent): Unit = {
    WarsCoreAPI.getWPlayer(e.getEntity).game match {
      case Some(game) =>
        game.death(e)
      case _ =>
    }
  }
}
