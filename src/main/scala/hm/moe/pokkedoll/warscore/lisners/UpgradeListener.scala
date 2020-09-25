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
            //val key = ItemUtil.getItemKey(tool)
            val key = ItemUtil.getKey(tool)
            upgradeItem.list.get(if (upgradeItem.list.contains(key)) key else "else") match {
              case Some(value) =>
                val result = ItemUtil.getItem(value._1).getOrElse(UpgradeUtil.invalidItem).clone()
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
