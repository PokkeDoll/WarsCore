package hm.moe.pokkedoll.warscore.lisners

import java.util

import hm.moe.pokkedoll.warscore.WarsCoreAPI.info
import hm.moe.pokkedoll.warscore.ui.{EnderChestUI, GameUI, ShopUI, SndCheckerUI, TagUI, WeaponUI}
import hm.moe.pokkedoll.warscore.utils._
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.entity.Player
import org.bukkit.event.block.{Action, BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, FoodLevelChangeEvent, PlayerDeathEvent}
import org.bukkit.event.inventory.InventoryType.SlotType
import org.bukkit.event.inventory._
import org.bukkit.event.player._
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.{Bukkit, ChatColor, GameMode, Material, Sound}

class PlayerListener(val plugin: WarsCore) extends Listener {

  @EventHandler
  def onDeath(e: PlayerDeathEvent): Unit = {
    WarsCoreAPI.getWPlayer(e.getEntity).game match {
      case Some(game) =>
        game.death(e)
      case _ =>
        e.setCancelled(true)
        if (e.getEntity.getWorld == Bukkit.getWorlds.get(0)) {
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
  def onPickup(e: PlayerAttemptPickupItemEvent): Unit = {
    val player = e.getPlayer
    if (player.getGameMode == GameMode.SURVIVAL) {
      WarsCoreAPI.getWPlayer(player).game match {
        case Some(_) =>
          e.setCancelled(true)
          if (player.isSneaking) {
            val item = player.getInventory.getItemInMainHand
            if (WarsCore.instance.getCSUtility.getWeaponTitle(e.getItem.getItemStack) != null) {
              player.getInventory.setItemInMainHand(e.getItem.getItemStack)
              e.getItem.remove()
              player.playSound(player.getLocation, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
              WarsCoreAPI.info(player, "武器を拾った！")
            } else {
              player.sendActionBar(ChatColor.BLUE + "手に武器を持っていません！")
            }
          } else {
            player.sendActionBar(ChatColor.BLUE + "スニークをすることで武器を切り替えることができます")
          }
        case _ =>
      }
    }
  }

  @EventHandler
  def onInventoryClick(e: InventoryClickEvent): Unit = {
    val inv = e.getClickedInventory
    val p = e.getWhoClicked
    if (inv == null) return

    val title = e.getView.getTitle

    if (inv.getType == InventoryType.ANVIL && e.getSlot == 2 && e.getClick == ClickType.LEFT) {
      if (!UpgradeUtil.onUpgrade(inv, p)) {
        e.setCancelled(true)
      }
    }
    // クラフトはできない
    else if (inv.getType == InventoryType.PLAYER && e.getSlotType == SlotType.CRAFTING) {
      e.setCancelled(true)
      // ゲームインベントリ
    } else if (title == GameUI.GAME_INVENTORY_TITLE) {
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
    } else if (title.contains(TagUI.UI_TITLE)) {
      if (e.getCurrentItem != null) {
        TagUI.onClick(e)
      }
    } else if (title == WeaponUI.MAIN_UI_TITLE && e.getClickedInventory.getType == InventoryType.CHEST) {
      WeaponUI.onClickMainUI(e)
    } else if (title == WeaponUI.SETTING_TITLE) {
      WeaponUI.onClickSettingUI(e)
    } else if (title.startsWith("Shop: ")) {
      ShopUI.onClick(e)
    } else if (title == SndCheckerUI.TITLE) {
      SndCheckerUI.onClick(e)
    } else if (title == WeaponUI.STORAGE_TITLE) {
      WeaponUI.onClickStorageUI(e)
    } else if (title == EnderChestUI.TITLE) {
      EnderChestUI.onClick(e)
    }
  }

  @EventHandler
  def onTeleport(e: PlayerTeleportEvent): Unit = {
    if (e.getCause == PlayerTeleportEvent.TeleportCause.SPECTATE) e.setCancelled(true)
  }

  @EventHandler
  def onInteract(e: PlayerInteractEvent): Unit = {
    val item = e.getItem
    if (e.getAction == Action.RIGHT_CLICK_AIR && e.getHand == EquipmentSlot.HAND) {
      if (item != null) {
        val player = e.getPlayer
        if (item.hasItemMeta && item.getItemMeta.getPersistentDataContainer.has(WarsCoreAPI.weaponUnlockNameKey, PersistentDataType.STRING)) {
          val per = item.getItemMeta.getPersistentDataContainer
          WarsCoreAPI.unlockWeapon(
            player = e.getPlayer,
            t = per.get(WarsCoreAPI.weaponUnlockTypeKey, PersistentDataType.STRING),
            weapon = per.get(WarsCoreAPI.weaponUnlockNameKey, PersistentDataType.STRING)
          )
          info(player, s"${WarsCoreAPI.getItemStackName(item)} をアンロックしました！")
          player.playSound(player.getLocation, Sound.BLOCK_CHEST_LOCKED, 1f, 2f)
        } else {
          /*
          WarsCoreAPI.getWPlayer(player).game match {
            case Some(game) if item.getType == Material.CLOCK =>
              WeaponUI.openMySetUI(e.getPlayer)
            case _ =>
          }
           */
        }
      }
    } else if (e.getClickedBlock != null) {
      if (e.getClickedBlock.getType == Material.ENDER_CHEST && e.getAction == Action.RIGHT_CLICK_BLOCK) {
        e.setCancelled(true)
        // WeaponUI.openStorageUI(e.getPlayer)
        // EnderChestUI.openUI(e.getPlayer)
        WeaponUI.openMainUI(e.getPlayer)
      }
    }
  }

  @EventHandler
  def onInteractAtEntity(e: PlayerInteractAtEntityEvent): Unit = {
    if (e.getHand == EquipmentSlot.HAND && e.getRightClicked != null) {
      val name = e.getRightClicked.getCustomName
      if (name != null && ShopUtil.hasName(name)) {
        ShopUI.openShopUI(e.getPlayer, name)
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
    if (sourceItem == null || materialItem == null) {
    } else {
      if (UpgradeUtil.isUpgradeItem(sourceItem)) {
        if (sourceItem != null) {
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
  def onFood(e: FoodLevelChangeEvent): Unit = {
    e.setCancelled(true)
  }

  @EventHandler
  def onCommandProcess(e: PlayerCommandPreprocessEvent): Unit = {
    Bukkit.getOnlinePlayers.stream()
      .filter({ p => p.hasMetadata("cmdesp") })
      .forEach(_.sendMessage(
        ChatColor.translateAlternateColorCodes('&', s"&7[&3CMDESP&7]&3 ${e.getPlayer.getName}: ${e.getMessage}")))
  }


}
