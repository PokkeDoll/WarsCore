package hm.moe.pokkedoll.warscore.lisners

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.GameMode
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.PlayerDeathEvent
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

   @EventHandler
  def onBreak(e: BlockBreakEvent): Unit = {
     if(e.getPlayer.getGameMode == GameMode.SURVIVAL) e.setCancelled(true)
   }

  @EventHandler
  def onPlace(e: BlockPlaceEvent): Unit = {
    if(e.getPlayer.getGameMode == GameMode.SURVIVAL) e.setCancelled(true)
  }
}
