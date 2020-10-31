package hm.moe.pokkedoll.warscore.lisners

import java.util

import hm.moe.pokkedoll.warscore.utils.{EconomyUtil, EnderChestManager, ItemUtil, MerchantUtil, TagUtil, UpgradeUtil}
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import org.bukkit.{Bukkit, ChatColor, GameMode, Material}
import org.bukkit.event.block.{Action, BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, PlayerDeathEvent}
import org.bukkit.event.inventory.InventoryType.SlotType
import org.bukkit.event.inventory.{ClickType, InventoryClickEvent, InventoryCloseEvent, InventoryType, PrepareAnvilEvent}
import org.bukkit.event.player.{PlayerCommandPreprocessEvent, PlayerInteractAtEntityEvent, PlayerInteractEvent, PlayerItemHeldEvent, PlayerTeleportEvent, PlayerToggleSprintEvent}
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.scheduler.BukkitRunnable

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
  def onDamage(e: EntityDamageByEntityEvent): Unit = {
    e.getEntity match {
      case player: Player =>
        WarsCoreAPI.getWPlayer(player).game match {
          case Some(game) =>
            game.damage(e)
          case _ =>
        }
      case _ =>
    }

  }

  @EventHandler
  def onBreak(e: BlockBreakEvent): Unit = {
    if (e.getPlayer.getGameMode == GameMode.SURVIVAL) {
      WarsCoreAPI.getWPlayer(e.getPlayer).game match {
        case Some(game) =>
          game.break(e)
        case _ =>
          e.setCancelled(true)
      }
    }
  }

  @EventHandler
  def onPlace(e: BlockPlaceEvent): Unit = {
    if (e.getPlayer.getGameMode == GameMode.SURVIVAL) {
      WarsCoreAPI.getWPlayer(e.getPlayer).game match {
        case Some(game) =>
          game.place(e)
        case _ =>
          e.setCancelled(true)
      }
    }
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
      val item = e.getCurrentItem
      if (item != null) {
        //EnderChestManager.openEnderChest(p, e.getCurrentItem.getAmount)
        EnderChestManager.openEnderChest(p, EnderChestManager.parseChestId(item.getItemMeta.getDisplayName))
      }
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
  def onInventoryClose(e: InventoryCloseEvent): Unit = {
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
    val item = e.getItem
    if(e.getAction == Action.RIGHT_CLICK_AIR && e.getHand == EquipmentSlot.HAND) {
      if(item != null) {
        if(item.getType == Material.NAME_TAG) {
          e.setCancelled(true)
          //val t = TagUtil.getTagIdFromItemStack(item)
          val t = TagUtil.getTagKeyFromItemStack(item)
          e.getPlayer.sendMessage(s"$t を獲得しました！(大嘘)")
        // ぽっけコインを所持している
        } else if (EconomyUtil.COIN.isSimilar(item)) {
          val player = e.getPlayer
          if(player.isSneaking) {
            EconomyUtil.coin2ingot(player, item, item.getAmount/9)
          } else {
            EconomyUtil.coin2ingot(player, item, 1)
          }
        } else if (EconomyUtil.INGOT.isSimilar(item)) {
          val player = e.getPlayer
          if(player.isSneaking) {
            EconomyUtil.ingot2coin(player, item, if(item.getAmount >= 7) 7 else item.getAmount)
          } else {
            EconomyUtil.ingot2coin(player, item)
          }
        }
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
    // 強化素材となるアイテム
    val materialItem = inv.getItem(1)
    if(sourceItem == null || materialItem == null) {
      return
    } else {
      if(UpgradeUtil.isUpgradeItem(sourceItem)) {
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
            case _ =>
          }
        }
      }
      e.getInventory.setRepairCost(40)
    }
  }

  @EventHandler
  def onSprint(e: PlayerToggleSprintEvent): Unit = {
    val player = e.getPlayer
    if(player.getWorld.getName == WarsCoreAPI.LOBBY && player.getFoodLevel != 20) {
      player.setFoodLevel(20)
    } else if(player.getWorld.getName != WarsCoreAPI.LOBBY && player.getGameMode == GameMode.SURVIVAL) {
      new BukkitRunnable {
        override def run(): Unit = {
          if(e.isSprinting) {
            new BukkitRunnable {
              override def run(): Unit = {
                if(8 >=player.getFoodLevel || !player.isSprinting) {
                  cancel()
                } else {
                  player.setFoodLevel(player.getFoodLevel - 1)
                }
              }
            }.runTaskTimer(plugin, 0L, 30L)
          } else {
            new BukkitRunnable {
              override def run(): Unit = {
                if(!player.isSprinting) {
                  new BukkitRunnable {
                    override def run(): Unit = {
                      if(player.isSprinting || player.getFoodLevel == 20) {
                        cancel()
                      } else {
                        player.setFoodLevel(player.getFoodLevel + 1)
                      }
                    }
                  }.runTaskTimer(plugin, 0, 20L)
                }
              }
            }.runTaskLater(plugin, 30L)
          }
        }
      }.runTaskLater(plugin, 1L)
    }
  }

  @EventHandler
  def onCommandProcess(e: PlayerCommandPreprocessEvent): Unit = {
    Bukkit.getOnlinePlayers.stream()
      .filter({p => p.hasMetadata("cmdesp")})
      .forEach(_.sendMessage(
        ChatColor.translateAlternateColorCodes('&', s"&7[&3CMDESP&7]&3 ${e.getPlayer.getName}: ${e.getMessage}")))
  }
}
