package hm.moe.pokkedoll.warscore.lisners

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import hm.moe.pokkedoll.warscore.Registry.GAME_ID
import hm.moe.pokkedoll.warscore.WarsCoreAPI.info
import hm.moe.pokkedoll.warscore.features.Chests
import hm.moe.pokkedoll.warscore.games.Game
import hm.moe.pokkedoll.warscore.ui._
import hm.moe.pokkedoll.warscore.utils._
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit._
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.block.{Action, BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.event.entity.{EntityDamageByEntityEvent, FoodLevelChangeEvent, PlayerDeathEvent}
import org.bukkit.event.inventory.InventoryType.SlotType
import org.bukkit.event.inventory._
import org.bukkit.event.player._
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

class PlayerListener(val plugin: WarsCore) extends Listener {


  private val knockdownKey = new NamespacedKey(plugin, "knockdown")

  @EventHandler
  def onDeath(e: PlayerDeathEvent): Unit = {
    e.setKeepInventory(true)
    e.setKeepLevel(true)
    e.getEntity match {
      case player: Player =>
        /*
        ノックダウン処理
         */
        if(Game.isKnockdown(player)) {
          Game.setKnockdown(player, value = false)
          WarsCoreAPI.getWPlayer(player).game.foreach(_.onDeath(e))
        } else {
          Game.setKnockdown(player, value = true)
        }
        new BukkitRunnable {
          override def run(): Unit = {
            player.spigot().respawn()
          }
        }.runTaskLater(plugin, 1L)
      case _ =>
    }
  }

  @EventHandler
  def onRespawn(e: PlayerRespawnEvent): Unit = {
    if(Game.isKnockdown(e.getPlayer)) {
      e.setRespawnLocation(e.getPlayer.getLocation())
      Game.knockdown(e.getPlayer)
    }
    WarsCoreAPI.getWPlayer(e.getPlayer).game.foreach(game => {
      game.onRespawn(e)
    })
  }

  @EventHandler
  def onSneak(e: PlayerToggleSneakEvent): Unit = {
    val player = e.getPlayer
    if(Game.isKnockdown(player)) {
      e.setCancelled(true)
    }
  }


  @EventHandler
  def onPostRespawn(e: PlayerPostRespawnEvent): Unit = {
    WarsCoreAPI.getWPlayer(e.getPlayer).game.foreach(game => {
      game.onPostRespawn(e)
    })
  }

  @EventHandler
  def onDamage(e: EntityDamageByEntityEvent): Unit = {
    e.getEntity match {
      case player: Player =>
        WarsCoreAPI.getWPlayer(player).game match {
          case Some(game) =>
            game.onDamage(e)
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
          game.onBreak(e)
        case _ =>
          e.setCancelled(true)
      }
    }
  }

  @EventHandler
  def onPlace(e: BlockPlaceEvent): Unit = {
    val player = e.getPlayer
    if (player.getGameMode == GameMode.SURVIVAL) {
      WarsCoreAPI.getWPlayer(player).game match {
        case Some(game) =>
          game.onPlace(e)
        case _ =>
          e.setCancelled(true)
      }
    } else if (player.getGameMode == GameMode.CREATIVE) {

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
            val pickUpItem = e.getItem.getItemStack
            val item = player.getInventory.getItemInMainHand
            if (WarsCore.instance.getCSUtility.getWeaponTitle(pickUpItem) != null) {
              val weaponType = WarsCoreAPI.getWeaponTypeFromLore(pickUpItem)
              val contents = player.getInventory.getContents
              contents.indices.filterNot(i => contents(i) == null || contents(i).getType == Material.AIR).find(i => WarsCoreAPI.getWeaponTypeFromLore(contents(i)) == weaponType) match {
                case Some(i) =>
                  player.getInventory.setItem(i, pickUpItem)
                  e.getItem.remove()
                  player.playSound(player.getLocation, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
                  player.sendActionBar(ChatColor.BLUE + "武器を持ち替えた！")
                case None =>
                  player.getInventory.addItem(pickUpItem)
                  e.getItem.remove()
                  player.playSound(player.getLocation, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
                  player.sendActionBar(ChatColor.BLUE + "武器を拾った！")
              }
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

    // クラフトはできない
    if (inv.getType == InventoryType.PLAYER && e.getSlotType == SlotType.CRAFTING) {
      e.setCancelled(true)
      // ゲームインベントリ
    } else if (title == GameUI.GAME_INVENTORY_TITLE) {
      e.setCancelled(true)
      val icon = e.getCurrentItem
      if(icon == null || !icon.hasItemMeta) return
      Option(icon.getItemMeta.getPersistentDataContainer.get(GAME_ID, PersistentDataType.STRING))
        .flatMap(WarsCoreAPI.games.get) match {
        case Some(game) if p.isInstanceOf[Player] =>
          lazy val player = p.asInstanceOf[Player]
          Game.join(WarsCoreAPI.getWPlayer(player), game)
          // game.join(player)
          player.closeInventory()
        case None =>
      }
    } else if (title.contains(TagUI.UI_TITLE)) {
      if (e.getCurrentItem != null) {
        TagUI.onClick(e)
      }
    } else if (title == WeaponUI.MAIN_UI_TITLE && e.getClickedInventory.getType == InventoryType.CHEST) {
      e.setCancelled(true)
      WeaponUI.onClickMainUI(e.getWhoClicked, e.getSlot)
    } else if (title == WeaponUI.SETTING_TITLE) {
      e.setCancelled(true)
      WeaponUI.onClickSettingUI(e.getWhoClicked, e.getClickedInventory, e.getCurrentItem, e.getSlot)
    } else if (title.startsWith("Shop: ")) {
      e.setCancelled(true)
      ShopUI.onClick(e.getWhoClicked.asInstanceOf[Player], e.getClickedInventory, e.getSlot, e.getCurrentItem, e.getView)
    } else if (title == SndCheckerUI.TITLE) {
      SndCheckerUI.onClick(e)
    } else if (title == WeaponUI.STORAGE_TITLE) {
      e.setCancelled(true)
      WeaponUI.onClickStorageUI(e.getWhoClicked, e.getCurrentItem, e.getSlot, e.isLeftClick, e.isRightClick)
    } else if (title == EnderChestUI.TITLE) {
      e.setCancelled(true)
      EnderChestUI.onClick(e.getWhoClicked, e.getSlot)
    } else if (title == GameUI.TIME_INVENTORY_TITLE) {
      e.setCancelled(true)
      GameUI.onClickTimeInventory(e.getClickedInventory, e.getSlot, e.getWhoClicked.asInstanceOf[Player])
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
        }
        // TODO ここにマイセット
      }
    } else if (e.getClickedBlock != null) {
      val block = e.getClickedBlock
      val getType = block.getType
      val player = e.getPlayer
      if (getType == Material.ENDER_CHEST && e.getAction == Action.RIGHT_CLICK_BLOCK) {
        e.setCancelled(true)
        WeaponUI.openMainUI(player)
      } else if (getType == Material.CHEST && e.getAction == Action.RIGHT_CLICK_BLOCK) {
        /*
        block match {
          case chest: Chest =>
            Chests.getLoot(chest.getLocation) match {
              case Right(loot) =>
                val i = chest.getInventory
                i.setItem(11, loot(0))
                i.setItem(13, loot(1))
                i.setItem(15, loot(2))
              case Left(error) =>
                player.sendMessage(error)
            }
        }
         */
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
