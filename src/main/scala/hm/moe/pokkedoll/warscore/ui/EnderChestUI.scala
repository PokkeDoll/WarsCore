package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.WarsCoreAPI.getNamedItemStack
import net.md_5.bungee.api.ChatColor
import org.bukkit.{Bukkit, Material}
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

object EnderChestUI {

  private lazy val db = WarsCore.instance.database

  val TITLE = ChatColor.DARK_PURPLE + "エンダーチェスト"

  private val inventory = {
    val inv = Bukkit.createInventory(null, 9, TITLE)
    inv.setItem(0, WarsCoreAPI.UI.CLOSE)
    inv.setItem(1, getNamedItemStack(Material.BOW, ChatColor.YELLOW + "プライマリ武器を表示する"))
    inv.setItem(2, getNamedItemStack(Material.ARROW, ChatColor.YELLOW + "セカンダリ武器を表示する"))
    inv.setItem(3, getNamedItemStack(Material.IRON_SWORD, ChatColor.YELLOW + "近接武器を表示する"))
    inv.setItem(4, getNamedItemStack(Material.POTION, ChatColor.YELLOW + "グレネードを表示する"))
    inv.setItem(5, getNamedItemStack(Material.PLAYER_HEAD, ChatColor.YELLOW + "帽子を表示する"))

    inv.setItem(6, getNamedItemStack(Material.GOLD_NUGGET, ChatColor.YELLOW + "アイテムを表示する"))
    inv
  }

  def openUI(player: HumanEntity): Unit = {
    player.openInventory(inventory)
  }

  /**
   * インベントリをクリックしたとき
   * clickedInventoryは存在する！
   *
   * @param e InventoryClickEvent
   */
  def onClick(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    val player = e.getWhoClicked
    e.getSlot match {
      case 1 =>
        WeaponUI.openStorageUI(player, 1, "primary")
      case 2 =>
        WeaponUI.openStorageUI(player, 1, "secondary")
      case 3 =>
        WeaponUI.openStorageUI(player, 1, "melee")
      case 4 =>
        WeaponUI.openStorageUI(player, 1, "grenade")
      case 5 =>
        WeaponUI.openStorageUI(player, 1, "head")
      case 6 =>
        WeaponUI.openStorageUI(player, 1, "item")
      case _ =>
    }
  }

}