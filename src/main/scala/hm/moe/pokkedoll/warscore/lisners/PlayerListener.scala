package hm.moe.pokkedoll.warscore.lisners

import hm.moe.pokkedoll.warscore.utils.{BankManager, EnderChestManager, MerchantUtil, TagUtil}
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.entity.Player
import org.bukkit.{Bukkit, ChatColor, GameMode, Material}
import org.bukkit.event.block.{Action, BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryType.SlotType
import org.bukkit.event.inventory.{InventoryClickEvent, InventoryCloseEvent, InventoryType}
import org.bukkit.event.player.{PlayerInteractAtEntityEvent, PlayerInteractEvent, PlayerItemHeldEvent, PlayerTeleportEvent}
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.inventory.EquipmentSlot

class PlayerListener(plugin: WarsCore) extends Listener {

  @EventHandler
  def onDeath(e: PlayerDeathEvent): Unit = {
    WarsCoreAPI.getWPlayer(e.getEntity).game match {
      case Some(game) =>
        game.death(e)
      case _ =>
        e.setCancelled(true)
        if(e.getEntity.getWorld == Bukkit.getWorlds.get(0)) {
          e.getEntity.teleport(e.getEntity.getWorld.getSpawnLocation)
        }
    }
  }

  @EventHandler
  def onBreak(e: BlockBreakEvent): Unit = {
    if (e.getPlayer.getGameMode == GameMode.SURVIVAL) e.setCancelled(true)
  }

  @EventHandler
  def onPlace(e: BlockPlaceEvent): Unit = {
    if (e.getPlayer.getGameMode == GameMode.SURVIVAL) e.setCancelled(true)
  }

  @EventHandler
  def onInventoryClick(e: InventoryClickEvent): Unit = {
    if (e.getClickedInventory == null) return
    if (e.getSlotType == SlotType.CRAFTING) {
      e.setCancelled(true)
      return
    } else if (e.getView.getTitle == WarsCoreAPI.GAME_INVENTORY_TITLE) {
      e.setCancelled(true)
      val icon = e.getCurrentItem
      if (icon == null || !icon.hasItemMeta || !icon.getItemMeta.hasDisplayName) return
      WarsCoreAPI.games.get(ChatColor.stripColor(icon.getItemMeta.getDisplayName)) match {
        case Some(game) if e.getWhoClicked.isInstanceOf[Player] =>
          lazy val player = e.getWhoClicked.asInstanceOf[Player]
          game.join(player)
          player.closeInventory()
        case None =>
      }
    } else if (e.getView.getTitle == EnderChestManager.ENDER_CHEST_MENU.getTitle) {
      if (e.getCurrentItem != null) {
        EnderChestManager.openEnderChest(e.getWhoClicked, e.getCurrentItem.getAmount)
      }
    } else if (e.getView.getTitle == BankManager.BANK_MENU.getTitle) {
      BankManager.onClick(e)
    } else {
      val wp = WarsCoreAPI.getWPlayer(e.getWhoClicked.asInstanceOf[Player])
      if (wp.game.isDefined) {
        if (!wp.changeInventory && e.getSlotType != SlotType.QUICKBAR) {
          e.setCancelled(true)
          e.getWhoClicked.sendMessage(ChatColor.RED + "インベントリを変更することはできません！")
        }
      }
    }
  }

  @EventHandler
  def onCInventoryClose(e: InventoryCloseEvent): Unit = {
    if (e.getInventory != null && e.getPlayer != null) {
      if (e.getInventory.getTitle.contains(ChatColor.DARK_PURPLE + e.getPlayer.getName + "'s Chest")) {
        val id = e.getInventory.getTitle.replaceAll(ChatColor.DARK_PURPLE + e.getPlayer.getName + "'s Chest ", "").toInt
        EnderChestManager.closeEnderChest(e.getPlayer, id, e.getInventory.getContents)
      }
    }
  }

  @EventHandler
  def onTeleport(e: PlayerTeleportEvent): Unit = {
    if (e.getCause == PlayerTeleportEvent.TeleportCause.SPECTATE) e.setCancelled(true)
  }

  @EventHandler
  def onInteract(e: PlayerInteractEvent): Unit = {
    if(e.getAction == Action.RIGHT_CLICK_AIR && e.getHand == EquipmentSlot.HAND) {
      val item = e.getItem
      if(item != null && item.getType == Material.NAME_TAG) {
        e.setCancelled(true)
        val t = TagUtil.getTagIdFromItemStack(item)
        e.getPlayer.sendMessage(s"$t を獲得しました！(大嘘)")
      }
    }
  }

  @EventHandler
  def onInteractAtEntity(e: PlayerInteractAtEntityEvent): Unit = {
    if(e.getHand == EquipmentSlot.HAND && e.getRightClicked != null && e.getRightClicked.getCustomName != null) {
      if(MerchantUtil.hasName(e.getRightClicked.getCustomName)) {
        MerchantUtil.openMerchantInventory(e.getPlayer, e.getRightClicked.getCustomName)
      }
    }
  }
}
