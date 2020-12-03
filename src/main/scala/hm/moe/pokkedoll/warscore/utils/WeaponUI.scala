package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.{Callback, WarsCore}
import net.md_5.bungee.api.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{ClickType, InventoryAction, InventoryClickEvent, InventoryCloseEvent, InventoryType}
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

  def openWeaponStorageUI(player: HumanEntity, page: Int = 1): Unit = {
    val inv = Bukkit.createInventory(null, 54, WEAPON_CHEST_UI_TITLE)
    (0 to 8).filterNot(_ == 4).foreach(inv.setItem(_, PANEL))
    val p = pageIcon(page)
    val baseSlot = (page - 1) * 45

    db.getPagedWeaponStorage(player.getUniqueId.toString, baseSlot, new Callback[mutable.Buffer[(Int, Array[Byte], Int)]] {
      override def success(value: mutable.Buffer[(Int, Array[Byte], Int)]): Unit = {
        inv.setItem(4, p)
        value.foreach(f => {
          println(f)
          val i = if (f._2 == null) new ItemStack(Material.AIR) else ItemStack.deserializeBytes(f._2)
          if(f._3 != 0) {
            i.addEnchantment(Enchantment.BINDING_CURSE, 10)
          }
          inv.setItem(9 + f._1 - baseSlot, i)
        })
      }
      override def failure(error: Exception): Unit = {
        (9 until 54).foreach(inv.setItem(_, ERROR_PANEL))
      }
    })
    player.openInventory(inv)
  }

  def onClickWeaponStorageUI(e: InventoryClickEvent): Unit = {
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
              openWeaponStorageUI(e.getWhoClicked, page = page + 1)
            } else if (e.getClick == ClickType.LEFT) {
              if (page != 1) openWeaponStorageUI(e.getWhoClicked, page = page - 1)
            }
          }
        }
      case _ =>
    }
  }

  // 1 ~ 8までは使用済みなのを忘れずに！
  def onCloseWeaponStorageUI(e: InventoryCloseEvent): Unit = {
    val player = e.getPlayer
    val inv = e.getView.getTopInventory
    val pageItem = inv.getItem(4)
    if(pageItem != null) {
      val page = pageItem.getItemMeta.getPersistentDataContainer.get(UI_PAGE_KEY, PersistentDataType.INTEGER)
      val baseSlot = (page - 1) * 45
      println(s"page is $page $baseSlot")
      // TODO 用テスト AIRやNULLはforeachの対象か？
      val mappedInv = (0 until 45).map(f => (f, inv.getItem(9 + f)))
      val groupedInv = mappedInv.groupBy(f => f._2 == null || f._2.getType == Material.AIR)
      db.setPagedWeaponStorage(player.getUniqueId.toString, baseSlot, groupedInv)
    }
  }
}
