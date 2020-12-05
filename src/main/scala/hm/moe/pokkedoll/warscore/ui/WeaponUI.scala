package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.{Callback, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{ClickType, InventoryClickEvent, InventoryCloseEvent, InventoryType}
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.{Bukkit, Material, NamespacedKey}

import scala.collection.mutable

object WeaponUI {

  private lazy val db = WarsCore.instance.database

  private val PANEL = {
    val i = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.GRAY + "-")
    i.setItemMeta(m)
    i
  }

  private val EMPTY = {
    val i = new ItemStack(Material.BARRIER)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.RED + "まだ設定されていません!")
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

  val MAIN: String = ChatColor.translateAlternateColorCodes('&', "&e&lMain")
  val SUB: String = ChatColor.translateAlternateColorCodes('&', "&e&lSub")
  val MELEE: String = ChatColor.translateAlternateColorCodes('&', "&e&lMelee")
  val ITEM: String = ChatColor.translateAlternateColorCodes('&', "&e&lItem")

  val WEAPON_TYPES: Array[String] = Array(MAIN, SUB, MELEE, ITEM)

  val MAIN_UI_TITLE: String = ChatColor.of("#000080") + "" + ChatColor.BOLD + "Weapon Setting Menu"

  def openMainUI(player: HumanEntity): Unit = {
    val inv = Bukkit.createInventory(null, 54, MAIN_UI_TITLE)
    inv.setContents(Array.fill(54)(PANEL))
    inv.setItem(10, new ItemStack(Material.IRON_SWORD))
    inv.setItem(19, new ItemStack(Material.SHIELD))
    inv.setItem(28, new ItemStack(Material.CROSSBOW))
    inv.setItem(37, new ItemStack(Material.HONEY_BOTTLE))

    inv.setItem(13, new ItemStack(Material.CHEST))
    inv.setItem(16, new ItemStack(Material.BARRIER))
    player.openInventory(inv)
    db.getWeapon(player.getUniqueId.toString, new Callback[mutable.Buffer[Array[Byte]]] {
      override def success(value: mutable.Buffer[Array[Byte]]): Unit = {
        val items = value.map(f => if (f == null) new ItemStack(Material.AIR) else ItemStack.deserializeBytes(f))
        val main = items.find(p => p.hasItemMeta && p.getItemMeta.hasLore && p.getItemMeta.getLore.stream().anyMatch(pred => pred.contains(MAIN))).getOrElse(EMPTY)
        val sub = items.find(p => p.hasItemMeta && p.getItemMeta.hasLore && p.getItemMeta.getLore.stream().anyMatch(pred => pred.contains(SUB))).getOrElse(EMPTY)
        val melee = items.find(p => p.hasItemMeta && p.getItemMeta.hasLore && p.getItemMeta.getLore.stream().anyMatch(pred => pred.contains(MELEE))).getOrElse(EMPTY)
        val item = items.find(p => p.hasItemMeta && p.getItemMeta.hasLore && p.getItemMeta.getLore.stream().anyMatch(pred => pred.contains(ITEM))).getOrElse(EMPTY)

        inv.setItem(11, main)
        inv.setItem(20, sub)
        inv.setItem(29, melee)
        inv.setItem(38, item)

        WarsCoreAPI.getWPlayer(player.asInstanceOf[Player]).weapons = Some(Array(main, sub, melee, item))
      }

      override def failure(error: Exception): Unit = {
        error.printStackTrace()
        player.sendMessage("エラー！")
      }
    })
  }

  /**
   * InventoryTypeはCHESTであることがわかる
   *
   * @param e イベント
   */
  def onClickMainUI(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    val player = e.getWhoClicked
    e.getSlot match {
      case 10 => openSettingUI(player)
      case 19 => openSettingUI(player, weaponType = 1)
      case 28 => openSettingUI(player, weaponType = 2)
      case 37 => openSettingUI(player, weaponType = 3)

      case 13 => openWeaponStorageUI(player)
      case 16 => player.closeInventory()
      case _ =>
    }
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
          if (f._3 != 0) {
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

  val SETTING_TITLE = "Weapon Settings"

  val weaponTypeKey = new NamespacedKey(WarsCore.instance, "weapon-type")
  val weaponUsingKey = new NamespacedKey(WarsCore.instance, "weapon-using")

  /**
   * 武器設定UIを開く
   *
   * @param player 対象のプレイヤー
   * @param page   ページ番号
   */
  def openSettingUI(player: HumanEntity, page: Int = 1, weaponType: Int = 0): Unit = {
    val inv = Bukkit.createInventory(null, 54, SETTING_TITLE)
    (0 to 8).filterNot(_ == 4).foreach(inv.setItem(_, PANEL))
    val p = {
      val ico = pageIcon(page)
      val m = ico.getItemMeta
      m.getPersistentDataContainer.set(weaponTypeKey, PersistentDataType.INTEGER, java.lang.Integer.valueOf(weaponType))
      ico.setItemMeta(m)
      ico
    }
    val baseSlot = (page - 1) * 45
    db.getPagedWeaponStorage(player.getUniqueId.toString, baseSlot, new Callback[mutable.Buffer[(Int, Array[Byte], Int)]] {
      // Slot, Item(Byte), Use の順！
      override def success(value: mutable.Buffer[(Int, Array[Byte], Int)]): Unit = {
        inv.setItem(4, p)
        value.foreach(f => {
          val i = if (f._2 == null) new ItemStack(Material.AIR) else ItemStack.deserializeBytes(f._2)
          if (i.getType != Material.AIR && i.hasItemMeta) {
            val m = i.getItemMeta
            if (m.hasLore && m.getLore.stream().anyMatch(pred => pred.contains(WEAPON_TYPES(weaponType)))) {
              if (f._3 != 0) {
                i.addUnsafeEnchantment(Enchantment.BINDING_CURSE, 10)
                m.setDisplayName("テスト: 現在の武器")
                i.setItemMeta(m)
              }
              inv.setItem(9 + f._1 - baseSlot, i)
            } else {
              inv.setItem(9 + f._1 - baseSlot, PANEL)
            }
          } else {
            inv.setItem(9 + f._1 - baseSlot, PANEL)
          }
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

  def onClickSettingUI(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    val player = e.getWhoClicked
    val inv = e.getClickedInventory
    inv.getType match {
      case InventoryType.CHEST =>
        val i = e.getCurrentItem
        val pageItem = inv.getItem(4)
        if (pageItem != null && i != null && i.getType != Material.AIR && i.getType != PANEL.getType) {
          val page = pageItem.getItemMeta.getPersistentDataContainer.get(UI_PAGE_KEY, PersistentDataType.INTEGER)
          val baseSlot = (page - 1) * 45
          db.setPagedWeapon(player.getUniqueId.toString, baseSlot + (e.getSlot - 9), new Callback[Unit] {
            override def success(value: Unit): Unit = {
              openMainUI(player)
            }

            override def failure(error: Exception): Unit = {
              error.printStackTrace()
              player.sendMessage("エラー！")
            }
          })
        }
      case _ =>
    }
  }

  // 1 ~ 8までは使用済みなのを忘れずに！
  def onCloseWeaponStorageUI(e: InventoryCloseEvent): Unit = {
    val player = e.getPlayer
    val inv = e.getView.getTopInventory
    val pageItem = inv.getItem(4)
    if (pageItem != null) {
      val page = pageItem.getItemMeta.getPersistentDataContainer.get(UI_PAGE_KEY, PersistentDataType.INTEGER)
      val baseSlot = (page - 1) * 45
      println(s"page is $page $baseSlot")
      val mappedInv = (0 until 45).map(f => (f, inv.getItem(9 + f)))
      val groupedInv = mappedInv.groupBy(f => f._2 == null || f._2.getType == Material.AIR)
      db.setPagedWeaponStorage(player.getUniqueId.toString, baseSlot, groupedInv)
    }
  }
}
