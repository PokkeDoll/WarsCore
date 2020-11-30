package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.WarsCore
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.{Bukkit, Material, NamespacedKey}
import org.bukkit.inventory.{Inventory, ItemStack}
import org.bukkit.persistence.PersistentDataType

object WeaponUI {

  private val PANEL = {
    val i = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
    val m = i.getItemMeta
    m.setDisplayName(" ")
    i.setItemMeta(m)
    i
  }

  val BASE_WEAPON_UI_TITLE: String = ChatColor.of("#000080") + "" + ChatColor.BOLD + "Weapon Setting Menu"

  def openMainUI(player: Player): Unit = {
    val inv = Bukkit.createInventory(null, 54, BASE_WEAPON_UI_TITLE)
    inv.setContents(Array.fill(54)(PANEL))
    inv.setItem(10, new ItemStack(Material.IRON_SWORD))
    inv.setItem(19, new ItemStack(Material.SHIELD))
    inv.setItem(28, new ItemStack(Material.CROSSBOW))
    inv.setItem(37, new ItemStack(Material.HONEY_BOTTLE))

    inv.setItem(13, new ItemStack(Material.CHEST))
    inv.setItem(16, new ItemStack(Material.BARRIER))
    player.openInventory(inv)
  }

  val UI_PAGE_KEY = new NamespacedKey(WarsCore.instance, "weapon-ui-page")

  private val pageIcon = (page: Int) => {
    val i = new ItemStack(Material.FILLED_MAP)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.translateAlternateColorCodes('&', s"&e${page - 1} &7← &a&l$page &r&7→ &e${page + 1}"))
    m.getPersistentDataContainer.set(UI_PAGE_KEY, PersistentDataType.INTEGER, page)
    i.setItemMeta(m)
    i
  }

  val WEAPON_CHEST_UI_TITLE = "Weapon Chest"

  def openWeaponChestUI(player: Player, page: Int = 1): Unit = {
    val inv = Bukkit.createInventory(null, 54, WEAPON_CHEST_UI_TITLE)
    inv.setItem(4, pageIcon(page))
    player.openInventory(inv)
  }
}
