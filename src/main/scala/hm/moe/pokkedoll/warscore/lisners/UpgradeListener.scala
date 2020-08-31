package hm.moe.pokkedoll.warscore.lisners

import java.util

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import hm.moe.pokkedoll.warscore.utils.{ItemUtil, UpgradeUtil}
import org.bukkit.event.inventory.{ClickType, InventoryClickEvent, InventoryType, PrepareAnvilEvent}
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.inventory.ItemStack
import org.bukkit.{ChatColor, Material, Sound}

class UpgradeListener extends Listener {
  @EventHandler
  def onClick(e: InventoryClickEvent): Unit = {
    val player = e.getWhoClicked
    if (e.getClickedInventory != null) {
      val inv = e.getClickedInventory
      val title = e.getView.getTitle
      if (inv.getType == InventoryType.ANVIL) {
        player.sendMessage(ChatColor.GRAY + "Wars互換モード: true")
        if (e.getSlot == 2 && e.getClick == ClickType.LEFT) {
          val item = inv.getItem(0)
          val tool = inv.getItem(1)
          if (item == null || tool == null) {
            e.setCancelled(true)
            return
          }
          UpgradeUtil.getUpgradeItem(item) match {
            case Some(upgradeItem) =>
              val result = inv.getItem(2)
              if (result == null && result.getType == Material.AIR) {
                e.setCancelled(true)
                return
              }
              val chance: Double = if (result.getItemMeta.hasLore) result.getItemMeta.getLore.get(0).replaceAll("§f成功確率: §a", "").replaceAll("%", "").toDouble else 0.0
              if (WarsCoreAPI.randomChance(chance)) {
                inv.setItem(0, new ItemStack(Material.AIR))
                inv.setItem(1, new ItemStack(Material.AIR))
                inv.setItem(2, new ItemStack(Material.AIR))
                val key = ItemUtil.getItemKey(tool)
                val success = if (upgradeItem.list.contains(key)) upgradeItem.list.get(key) else upgradeItem.list.get("else")
                ItemUtil.items.get(success.getOrElse(("", 0.0))._1) match {
                  case Some(value) =>
                    player.setItemOnCursor(value)
                    player.sendMessage("§9成功しました!")
                    player.getWorld.playSound(player.getLocation, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f)
                    player.getWorld.playSound(player.getLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f)
                    WarsCoreAPI.spawnFirework(player.getLocation)
                  case None =>
                }
              } else {
                player.sendMessage("§c失敗しました...")
                inv.setItem(1, new ItemStack(Material.AIR))
                inv.setItem(2, new ItemStack(Material.AIR))
                player.getWorld.playSound(player.getLocation, Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f)
              }
            case None =>
          }
        }
      }
    }
  }
  
  @EventHandler
  def onAnvilPrepare(e: PrepareAnvilEvent): Unit = {
    // 元となるアイテム
    val item = e.getInventory.getItem(0)
    if (item == null) return
    if (UpgradeUtil.isUpgradeItem(item)) {
      // 強化素材となるアイテム
      val tool = e.getInventory.getItem(1)
      if (tool != null) {
        // 素材が持つ成功確率
        val baseChance: Double = UpgradeUtil.getChance(tool)
        UpgradeUtil.getUpgradeItem(item) match {
          case Some(upgradeItem) =>
            val key = ItemUtil.getItemKey(tool)
            upgradeItem.list.get(if (upgradeItem.list.contains(key)) key else "else") match {
              case Some(value) =>
                val result = ItemUtil.items.getOrElse(value._1, UpgradeUtil.invalidItem).clone()
                val rMeta = result.getItemMeta
                val chance = if (baseChance - value._2 > 0) baseChance - value._2 else 0.0
                rMeta.setLore(util.Arrays.asList(s"§f成功確率: §a${chance * tool.getAmount}%", "§4§n確率で失敗します!!"))
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
