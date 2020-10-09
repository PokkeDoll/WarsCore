package hm.moe.pokkedoll.warscore.utils

import java.util

import hm.moe.pokkedoll.warscore.Test
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{ClickType, InventoryAction, InventoryClickEvent}
import org.bukkit.event.inventory.InventoryType.SlotType
import org.bukkit.inventory.ItemStack
import org.bukkit.{Bukkit, ChatColor, Material}

object BankManager {

  val BANK_MENU = {
    val inv = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Bank Menu!")
    val coin = {
      val i = new ItemStack(Material.GOLD_NUGGET, 1)
      val m = i.getItemMeta
      m.setDisplayName(ChatColor.DARK_AQUA + "ぽっけコイン" + ChatColor.WHITE + "に変換！")
      m.setLore(util.Arrays.asList(""))
      i.setItemMeta(m)
      i
    }
    val ingot = {
      val i = new ItemStack(Material.GOLD_INGOT, 1)
      i
    }
    val penel = {
      val i = new ItemStack(Material.STAINED_GLASS_PANE, 1, 4)
      val m = i.getItemMeta
      m.setDisplayName(" ")
      i.setItemMeta(m)
      i
    }
    (0 to 26).filterNot(i => i == 11 || i == 15).foreach(inv.setItem(_, penel))
    inv.setItem(11, coin)
    inv.setItem(15, ingot)
    inv
  }

  def openBankInventory(player: Player): Unit = {
    player.openInventory(BANK_MENU)
  }

  def onClick(e: InventoryClickEvent): Unit = {
    val test = new Test("BankManager.onClick")
    e.setCancelled(true)
    if (e.getSlotType != SlotType.CONTAINER) return
    if (e.getSlot == 12) {
      if (e.getClick == ClickType.LEFT) {
        coin2ingot(e.getWhoClicked, 1)
      } else if (e.getClick == ClickType.SHIFT_LEFT) {
        coin2ingot(e.getWhoClicked, 10)
      }
    } else if (e.getSlot == 16) {

    }
    test.log()
  }

  /**
   * インベントリにあるコインをインゴットに変換する
   *
   * @param player
   * @param amount
   */
  def coin2ingot(player: HumanEntity, amount: Int): Unit = {
    val inv = player.getInventory
    if (inv.firstEmpty() == -1) {
      player.sendMessage(ChatColor.RED + "インベントリの空きが不足しています！")
    } else {
      val coin = EconomyUtil.COIN.clone()
      coin.setAmount(9 * amount)
      if (inv.contains(coin)) {
        inv.remove(coin)
        val ingot = EconomyUtil.INGOT.clone()
        ingot.setAmount(amount)
        inv.addItem(ingot)
      } else {
        //TODO コインを持っていない
        player.sendMessage(ChatColor.RED + "コインを所持していません！")
      }
    }
  }
}
