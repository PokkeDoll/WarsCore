package hm.moe.pokkedoll.warscore.lisners

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.entity.Player
import org.bukkit.{ChatColor, GameMode}
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.{InventoryClickEvent, InventoryCloseEvent}
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

  @EventHandler
  def onInventoryClick(e: InventoryClickEvent): Unit = {
    if(e.getClickedInventory == null) return
    if(e.getView.getTitle == WarsCoreAPI.GAME_INVENTORY_TITLE) {
      e.setCancelled(true)
      val icon = e.getCurrentItem
      if(icon == null || !icon.hasItemMeta || !icon.getItemMeta.hasDisplayName) return
      WarsCoreAPI.games.get(ChatColor.stripColor(icon.getItemMeta.getDisplayName)) match {
        case Some(game) if e.getWhoClicked.isInstanceOf[Player] =>
          lazy val player = e.getWhoClicked.asInstanceOf[Player]
          game.join(player)
          player.closeInventory()
        case None =>
      }
    }
  }
}
