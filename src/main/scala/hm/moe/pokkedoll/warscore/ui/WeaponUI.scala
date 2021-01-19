package hm.moe.pokkedoll.warscore.ui

import java.util

import hm.moe.pokkedoll.warscore.db.WeaponDB
import hm.moe.pokkedoll.warscore.utils.ItemUtil
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{InventoryClickEvent, InventoryType}
import org.bukkit.inventory.{ItemFlag, ItemStack}
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, Material, NamespacedKey}

object WeaponUI {

  private lazy val db = WarsCore.instance.database

  private val PANEL = {
    val i = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.GRAY + "-")
    i.setItemMeta(m)
    i
  }

  val EMPTY: ItemStack = {
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

  private val BACK_MAIN_UI = {
    val i = new ItemStack(Material.ITEM_FRAME)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.RED + "メイン設定に戻る")
    i.setItemMeta(m)
    i
  }

  private val OPEN_CHEST_ICON = {
    val i = new ItemStack(Material.CHEST)
    val m = i.getItemMeta
    m.setDisplayName("チェストを開く")
    m.setLore(java.util.Arrays.asList())
    i.setItemMeta(m)
    i
  }

  private val CLOSE_INVENTORY_ICON = {
    val i = new ItemStack(Material.BARRIER)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.RED + "インベントリを閉じる")
    m.setLore(java.util.Arrays.asList())
    i.setItemMeta(m)
    i
  }

  private val OPEN_MY_SET_ICON = {
    val i = new ItemStack(Material.CRAFTING_TABLE)
    val m = i.getItemMeta
    m.setDisplayName("マイセットを開く")
    m.setLore(java.util.Arrays.asList())
    i.setItemMeta(m)
    i
  }


  val MAIN: String = ChatColor.translateAlternateColorCodes('&', "&e&lMain")
  val SUB: String = ChatColor.translateAlternateColorCodes('&', "&e&lSub")
  val MELEE: String = ChatColor.translateAlternateColorCodes('&', "&e&lMelee")
  val ITEM: String = ChatColor.translateAlternateColorCodes('&', "&e&lItem")

  val MAIN_UI_TITLE: String = ChatColor.of("#000080") + "" + ChatColor.BOLD + "Weapon Setting Menu"

  def openMainUI(player: HumanEntity): Unit = {
    val inv = Bukkit.createInventory(null, 27, MAIN_UI_TITLE)
    inv.setContents(Array.fill(27)(PANEL))

    inv.setItem(15, OPEN_CHEST_ICON)
    inv.setItem(16, OPEN_MY_SET_ICON)
    inv.setItem(17, CLOSE_INVENTORY_ICON)

    player.openInventory(inv)

    new BukkitRunnable {
      override def run(): Unit = {
        val weapons = db.getActiveWeapon(player.getUniqueId.toString)
        val weaponIcon = (item: ItemStack) => {
          val i = item.clone()
          val m = i.getItemMeta
          m.setLore(java.util.Arrays.asList(
            ChatColor.RED + "" + ChatColor.UNDERLINE + "変更するにはクリックしてください"
          ))
          i.setItemMeta(m)
          i
        }
        inv.setItem(10, weaponIcon(ItemUtil.getItem(weapons._1).getOrElse(EMPTY)))
        inv.setItem(11, weaponIcon(ItemUtil.getItem(weapons._2).getOrElse(EMPTY)))
        inv.setItem(12, weaponIcon(ItemUtil.getItem(weapons._3).getOrElse(EMPTY)))
        inv.setItem(13, weaponIcon(ItemUtil.getItem(weapons._4).getOrElse(EMPTY)))
      }
    }.runTask(WarsCore.instance)
  }

  /**
   * InventoryTypeはCHESTであることがわかる
   *
   * @param e イベント
   */
  def onClickMainUI(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    val player = e.getWhoClicked
    if (WarsCoreAPI.getWPlayer(player.asInstanceOf[Player]).game.isDefined) return
    e.getSlot match {
      case 10 => openSettingUI(player, weaponType = WeaponDB.PRIMARY)
      case 11 => openSettingUI(player, weaponType = WeaponDB.SECONDARY)
      case 12 => openSettingUI(player, weaponType = WeaponDB.MELEE)
      case 13 => openSettingUI(player, weaponType = WeaponDB.ITEM)

      //case 15 => openWeaponStorageUI(player)
      //case 16 => openMySetUI(player)
      case 17 => player.closeInventory()
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

  val SETTING_TITLE = "Weapon Settings"

  val weaponTypeKey = new NamespacedKey(WarsCore.instance, "weapon-type")

  private val weaponKey = new NamespacedKey(WarsCore.instance, "weapon-key")

  val STORAGE_TITLE = "Storage"

  /**
   * ストレージUIを開く
   *
   * @param player 対象のPlayer
   * @param page   ページ。デフォルトは1
   */
  def openStorageUI(player: HumanEntity, page: Int): Unit = {
    val inv = Bukkit.createInventory(null, 54, STORAGE_TITLE)
    val header = Array.fill(9)(PANEL)
    header.update(0, new ItemStack(Material.BARRIER))
    header.update(1, BACK_MAIN_UI)
    header.update(4, pageIcon(page))
    val offset = (page - 1) * 45
    new BukkitRunnable {
      override def run(): Unit = {
        inv.setContents(
          header ++
            db.getOriginalItem(player.getUniqueId.toString, offset)
              .map(data => {
                val item = ItemUtil.getItem(data._2).getOrElse(EMPTY).clone()
                val meta = item.getItemMeta
                val lore = new util.ArrayList[String](java.util.Arrays.asList(
                  ChatColor.WHITE + "【情報】",
                  ChatColor.GRAY + "" + ChatColor.BOLD + "タイプ: " + ChatColor.GREEN + data._1,
                  ChatColor.GRAY + "" + ChatColor.BOLD + "所持数: " + ChatColor.GREEN + data._3,
                  " ",
                  ChatColor.WHITE + "【説明】"
                ))
                if (meta.hasLore) lore.addAll(meta.getLore)
                meta.setLore(lore)
                item.setItemMeta(meta)
                if (data._4) {
                  item.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 0)
                  item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                }
                item
              })
              .toArray
        )
        player.openInventory(inv)
      }
    }.runTask(WarsCore.instance)
  }


  /**
   * ストレージUIを開く
   *
   * @param player 対象のPlayer
   * @param page   ページ。デフォルトは1
   */
  def openStorageUI(player: HumanEntity, page: Int, `type`: String): Unit = {
    val inv = Bukkit.createInventory(null, 54, STORAGE_TITLE)
    val header = Array.fill(9)(PANEL)
    header.update(0, new ItemStack(Material.BARRIER))
    header.update(1, BACK_MAIN_UI)
    header.update(4, pageIcon(page))
    val offset = (page - 1) * 45
    new BukkitRunnable {
      override def run(): Unit = {
        inv.setContents(
          header ++
            db.getOriginalItem(player.getUniqueId.toString, offset, `type`)
              .map(data => {
                val item = ItemUtil.getItem(data._1).getOrElse(EMPTY).clone()
                val meta = item.getItemMeta
                val lore = new util.ArrayList[String](java.util.Arrays.asList(
                  ChatColor.WHITE + "【情報】",
                  ChatColor.GRAY + "" + ChatColor.BOLD + "所持数: " + ChatColor.GREEN + data._2,
                  " ",
                  ChatColor.WHITE + "【説明】"
                ))
                if (meta.hasLore) lore.addAll(meta.getLore)
                meta.setLore(lore)
                item.setItemMeta(meta)
                if (data._3) {
                  item.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 0)
                  item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                }
                item
              })
              .toArray
        )
        player.openInventory(inv)
      }
    }.runTask(WarsCore.instance)
  }
  /**
   * ストレージUIをクリックしたときに呼ばれる
   *
   * @param e InventoryClickEvent
   */
  def onClickStorageUI(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    val player = e.getWhoClicked
    e.getSlot match {
      case 0 => player.closeInventory()
      case 1 => openMainUI(player)
      case 4 =>
        val i = e.getCurrentItem
        val page = i.getItemMeta.getPersistentDataContainer.get(UI_PAGE_KEY, PersistentDataType.INTEGER)
        if (e.isLeftClick) {
          if (page != 1) openStorageUI(player, page - 1)
        } else if (e.isRightClick) {
          openStorageUI(player, page + 1)
        }
      case _ =>
    }
  }

  /**
   * 武器設定UIを開く
   *
   * @param player 対象のプレイヤー
   * @param page   ページ番号
   */
  def openSettingUI(player: HumanEntity, page: Int = 1, weaponType: String): Unit = {
    if (WarsCoreAPI.getWPlayer(player.asInstanceOf[Player]).game.isDefined) return
    val inv = Bukkit.createInventory(null, 54, SETTING_TITLE)
    (0 to 8).filterNot(_ == 4).foreach(inv.setItem(_, PANEL))

    val p = pageIcon(page)

    val baseSlot = (page - 1) * 45
    new BukkitRunnable {
      override def run(): Unit = {
        val weapons = db.getWeapons(player.getUniqueId.toString, weaponType).slice(baseSlot, baseSlot + 45)
        inv.setItem(1, BACK_MAIN_UI)
        inv.setItem(4, p)
        val barrier = new ItemStack(Material.BARRIER)
        weapons.indices.foreach(f => {
          val item = weapons(f)
          val i = ItemUtil.getItem(item.name).getOrElse(EMPTY).clone()
          val m = i.getItemMeta
          m.setDisplayName(m.getDisplayName + " × " + item.amount)
          m.getPersistentDataContainer.set(weaponKey, PersistentDataType.STRING, item.name)
          m.getPersistentDataContainer.set(weaponTypeKey, PersistentDataType.STRING, weaponType)
          i.setItemMeta(m)
          inv.setItem(9 + f, i)
        })
        inv.setItem(0, barrier)
      }
    }.runTask(WarsCore.instance)
    player.openInventory(inv)
  }

  /**
   * openSettingUIに対して呼ばれる
   *
   * @param e InventoryClickEvent
   */
  def onClickSettingUI(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    val player = e.getWhoClicked
    val inv = e.getClickedInventory
    inv.getType match {
      case InventoryType.CHEST =>
        val i = e.getCurrentItem
        val pageItem = inv.getItem(4)
        val usedSlotItem = inv.getItem(0)
        // バリアブロック。インベントリを閉じる
        if (e.getSlot == 0) {
          player.closeInventory()
          // 額縁。メインメニューに戻る
        } else if (e.getSlot == 1) {
          openMainUI(player)
        } else if (i != null && i.getType != Material.AIR && i.getType != PANEL.getType) {
          val meta = i.getItemMeta
          val per = meta.getPersistentDataContainer
          if (per.has(weaponKey, PersistentDataType.STRING)) {
            db.setWeapon(player.getUniqueId.toString, t = per.get(weaponTypeKey, PersistentDataType.STRING), name = per.get(weaponKey, PersistentDataType.STRING))
            openMainUI(player)
          }
        }
      case _ =>
    }
  }


}
