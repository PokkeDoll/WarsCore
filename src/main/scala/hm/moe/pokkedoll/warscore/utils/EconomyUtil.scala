package hm.moe.pokkedoll.warscore.utils

import java.util.Collections

import org.bukkit.entity.Player
import org.bukkit.{ChatColor, Material}
import org.bukkit.inventory.ItemStack

/**
 * 金！をまとめるオブジェクト
 *
 * @author Emorard
 */
object EconomyUtil {
  val COIN = new WEconomy({
    val i = new ItemStack(Material.GOLD_NUGGET, 1)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.DARK_AQUA + "ぽっけコイン")
    m.setLore(Collections.singletonList(ChatColor.GRAY + "通貨としての価値がある"))
    i.setItemMeta(m)
    i
  })

  val INGOT = new WEconomy({
    val i = new ItemStack(Material.GOLD_INGOT, 1)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.GOLD + "ぽっけインゴット")
    m.setLore(Collections.singletonList(ChatColor.GRAY + "通貨としての価値がある"))
    i.setItemMeta(m)
    i
  })

  class WEconomy(val item: ItemStack)

  def give(player: Player, economy: WEconomy, amount: Int): Unit = {
    val item = economy.item.clone()
    item.setAmount(amount)
    player.getInventory.addItem(item)
  }
}

