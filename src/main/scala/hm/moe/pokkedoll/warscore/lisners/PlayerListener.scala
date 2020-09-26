package hm.moe.pokkedoll.warscore.lisners

import java.util

import hm.moe.pokkedoll.warscore.utils.{BankManager, EnderChestManager, ItemUtil, MerchantUtil, TagUtil, UpgradeUtil}
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.entity.Player
import org.bukkit.{Bukkit, ChatColor, GameMode, Material}
import org.bukkit.event.block.{Action, BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryType.SlotType
import org.bukkit.event.inventory.{ClickType, InventoryClickEvent, InventoryCloseEvent, InventoryType, PrepareAnvilEvent}
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
    val inv = e.getClickedInventory
    val p = e.getWhoClicked
    if (inv == null) return
    if(inv.getType == InventoryType.ANVIL && e.getSlot == 2 && e.getClick == ClickType.LEFT) {
      if(!UpgradeUtil.onUpgrade(inv, p)) {
        e.setCancelled(true)
        plugin.getLogger.info("Event Cancelled@47")
      }
    }
    // クラフトはできない
    else if (inv.getType == InventoryType.PLAYER && e.getSlotType == SlotType.CRAFTING) {
      e.setCancelled(true)
      plugin.getLogger.info("Event Cancelled!@54")
    // ゲームインベントリ
    } else if (e.getView.getTitle == WarsCoreAPI.GAME_INVENTORY_TITLE) {
      plugin.getLogger.info("Event Cancelled!@59")
      e.setCancelled(true)
      val icon = e.getCurrentItem
      if (icon == null || !icon.hasItemMeta || !icon.getItemMeta.hasDisplayName) return
      WarsCoreAPI.games.get(ChatColor.stripColor(icon.getItemMeta.getDisplayName)) match {
        case Some(game) if p.isInstanceOf[Player] =>
          lazy val player = p.asInstanceOf[Player]
          game.join(player)
          player.closeInventory()
        case None =>
      }
    // エンダーチェストインベントリ
    } else if (e.getView.getTitle == EnderChestManager.ENDER_CHEST_MENU.getTitle) {
      if (e.getCurrentItem != null) {
        EnderChestManager.openEnderChest(p, e.getCurrentItem.getAmount)
      }
    // 換金インベントリ
    } else if (e.getView.getTitle == BankManager.BANK_MENU.getTitle) {
      BankManager.onClick(e)
    // それ以外
    } else {
      val wp = WarsCoreAPI.getWPlayer(p.asInstanceOf[Player])
      if (wp.game.isDefined) {
        if (!wp.changeInventory && e.getSlotType != SlotType.QUICKBAR) {
          plugin.getLogger.info("Event Cancelled!@83")
          e.setCancelled(true)
          p.sendMessage(ChatColor.RED + "インベントリを変更することはできません！")
        }
      }
    }
  }

  @EventHandler
  def onCInventoryClose(e: InventoryCloseEvent): Unit = {
    val inv = e.getInventory
    val player = e.getPlayer
    if (inv != null && player != null) {
      if (inv.getTitle.contains(ChatColor.DARK_PURPLE + player.getName + "'s Chest")) {
        val id = inv.getTitle.replaceAll(ChatColor.DARK_PURPLE + player.getName + "'s Chest ", "").toInt
        EnderChestManager.closeEnderChest(player, id, inv.getContents)
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
    if(e.getHand == EquipmentSlot.HAND && e.getRightClicked != null) {
      val name = e.getRightClicked.getCustomName
      if(name!=null && MerchantUtil.hasName(name)) {
        MerchantUtil.openMerchantInventory(e.getPlayer, name)
      }
    }
  }

  @EventHandler
  def onAnvilPrepare(e: PrepareAnvilEvent): Unit = {
    val inv = e.getInventory
    // 元となるアイテム
    val sourceItem = inv.getItem(0)
    if(sourceItem == null) {
      return
    } else {
      if(UpgradeUtil.isUpgradeItem(sourceItem)) {
        // 強化素材となるアイテム
        val materialItem = inv.getItem(1)
        if(sourceItem != null) {
          val baseChance = UpgradeUtil.getChance(materialItem)
          UpgradeUtil.getUpgradeItem(sourceItem) match {
            case Some(upgradeItem) =>
              val key = ItemUtil.getKey(materialItem)
              upgradeItem.list.get(if (upgradeItem.list.contains(key)) key else "else") match {
                case Some(value) =>
                  val result = ItemUtil.getItem(value._1).getOrElse(UpgradeUtil.invalidItem).clone()
                  val rMeta = result.getItemMeta
                  val chance = if (baseChance - value._2 > 0) baseChance - value._2 else 0.0
                  rMeta.setLore(util.Arrays.asList(s"§f成功確率: §a${chance * materialItem.getAmount}%", "§4§n確率で失敗します!!"))
                  result.setItemMeta(rMeta)
                  e.setResult(result)
                  e.getInventory.setRepairCost(1)
                  return
                case _ =>
              }
            case None =>
          }
        }
      }
      e.getInventory.setRepairCost(40)
    }
  }
}
