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
  val COIN: ItemStack = {
    val i = new ItemStack(Material.GOLD_NUGGET, 1)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.DARK_AQUA + "ぽっけコイン")
    m.setLore(Collections.singletonList(ChatColor.GRAY + "通貨としての価値がある"))
    i.setItemMeta(m)
    i
  }

  val INGOT: ItemStack = {
    val i = new ItemStack(Material.GOLD_INGOT, 1)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.GOLD + "ぽっけインゴット")
    m.setLore(Collections.singletonList(ChatColor.GRAY + "通貨としての価値がある"))
    i.setItemMeta(m)
    i
  }

  def give(player: Player, economy: ItemStack, amount: Int): Unit = {
    val item = economy.clone()
    item.setAmount(amount)
    player.getInventory.addItem(item)
  }
}

