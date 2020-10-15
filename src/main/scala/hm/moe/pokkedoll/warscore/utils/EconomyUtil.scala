package hm.moe.pokkedoll.warscore.utils

import java.util.Collections

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.entity.Player
import org.bukkit.{ChatColor, Material}
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

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

  def coin2ingot(player: Player, item: ItemStack, amount: Int = 1): Unit = {
    if(item.isSimilar(COIN) && item.getAmount >= amount * 9) {
      val inv = player.getInventory
      if(inv.firstEmpty() == -1) {
        player.sendMessage(ChatColor.RED + "インベントリの空きが不足しています！")
      } else {
        item.setAmount(item.getAmount - amount*9)
        val ingot = EconomyUtil.INGOT.clone()
        ingot.setAmount(amount)
        inv.addItem(ingot)
      }
    } else {
      player.sendMessage(ChatColor.RED + "コインを所持していません！")
    }
  }

  /**
   * 最高7まで(63)
   * @param player
   * @param item
   * @param amount
   */
  def ingot2coin(player: Player, item: ItemStack, amount: Int = 1): Unit = {
    if(item.isSimilar(INGOT) && item.getAmount >= amount) {
      val inv = player.getInventory
      if(inv.firstEmpty() == -1) {
        player.sendMessage(ChatColor.RED + "インベントリの空きが不足しています！")
      } else {
        item.setAmount(item.getAmount - amount)
        new BukkitRunnable {
          override def run(): Unit = {
            val coin = EconomyUtil.COIN.clone()
            coin.setAmount(amount * 9)
            inv.addItem(coin)
          }
        }.runTaskLater(WarsCore.instance, 5L)
      }
    } else {
      player.sendMessage(ChatColor.RED + "コインを所持していません！")
    }
  }
}

