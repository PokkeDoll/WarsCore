package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.{Callback, WarsCore}
import net.md_5.bungee.api.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{ClickType, InventoryAction, InventoryClickEvent, InventoryType}
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.{Bukkit, Material, NamespacedKey}
import org.bukkit.inventory.{Inventory, ItemStack}
import org.bukkit.persistence.PersistentDataType

import scala.collection.mutable

object WeaponUI {

  lazy val db = WarsCore.instance.database

  private val PANEL = {
    val i = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
    val m = i.getItemMeta
    m.setDisplayName(" ")
    i.setItemMeta(m)
    i
  }

  private val ERROR_PANEL = {
    val i = new ItemStack(Material.BARRIER)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.RED + "-")
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

    // 武器セット
    inv.setItem(11, new ItemStack(Material.STONE))
    inv.setItem(20, new ItemStack(Material.STONE))
    inv.setItem(29, new ItemStack(Material.STONE))
    inv.setItem(38, new ItemStack(Material.STONE))

    inv.setItem(13, new ItemStack(Material.CHEST))
    inv.setItem(16, new ItemStack(Material.BARRIER))
    player.openInventory(inv)
  }

  val UI_PAGE_KEY = new NamespacedKey(WarsCore.instance, "weapon-ui-page")

  private val pageIcon = (page: Int) => {
    val i = new ItemStack(Material.WRITABLE_BOOK)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.translateAlternateColorCodes('&', s"&e${if (page == 1) "-" else page - 1} &7← &a&l$page &r&7→ &e${page + 1}"))
    m.setLore(java.util.Arrays.asList("左クリック | - | 右クリック"))
    m.getPersistentDataContainer.set(UI_PAGE_KEY, PersistentDataType.INTEGER, java.lang.Integer.valueOf(page))
    i.setItemMeta(m)
    i
  }

  val WEAPON_CHEST_UI_TITLE = "Weapon Chest"

  private val UI_ITEM_KEY = new NamespacedKey(WarsCore.instance, "weapon-ui-cache")

  def openWeaponChestUI(player: HumanEntity, page: Int = 1, cache: Option[Any] = None): Unit = {
    val inv = Bukkit.createInventory(null, 54, WEAPON_CHEST_UI_TITLE)
    (0 to 8).filterNot(_ == 4).foreach(inv.setItem(_, PANEL))
    val p = pageIcon(page)

    db.getWeaponChest(player.getUniqueId.toString, new Callback[mutable.Buffer[(String, Array[Byte], Boolean)]] {
      override def success(value: mutable.Buffer[(String, Array[Byte], Boolean)]): Unit = {
        inv.setItem(4, p)
        val items = value.slice((page - 1) * 45, ((page - 1) * 45) + 45)
        items.indices.foreach(f => {
          val i = if (items(f)._2 == null) new ItemStack(Material.AIR) else ItemStack.deserializeBytes(items(f)._2)
          // とりあえず表示
          inv.setItem(f, i)
        })
      }

      override def failure(error: Exception): Unit = {
        (9 until 54).foreach(inv.setItem(_, ERROR_PANEL))
      }
    })
    player.openInventory(inv)
  }

  def onClickWeaponChestUI(e: InventoryClickEvent): Unit = {
    e.getClickedInventory.getType match {
      case InventoryType.PLAYER if e.getClick == ClickType.SHIFT_LEFT =>
        e.getWhoClicked.sendMessage(e.getWhoClicked.getOpenInventory.getType.toString)
      case InventoryType.CHEST =>
        if (e.getSlot >= 0 && e.getSlot < 9) {
          e.setCancelled(true)
          if (e.getCurrentItem != null && e.getCurrentItem.getType == Material.BARRIER) {
            e.setCancelled(true)
          } else if (e.getSlot == 4) {
            val pageIcon = e.getClickedInventory.getItem(4)
            val page = pageIcon.getItemMeta.getPersistentDataContainer.get(UI_PAGE_KEY, PersistentDataType.INTEGER)
            if (e.getClick == ClickType.RIGHT) {
              openWeaponChestUI(e.getWhoClicked, page = page + 1)
            } else if (e.getClick == ClickType.LEFT) {
              if (page != 1) openWeaponChestUI(e.getWhoClicked, page = page - 1)
            }
          }
        }
      case _ =>
    }
  }
}
