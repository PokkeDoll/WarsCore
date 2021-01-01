package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.db.WeaponDB
import hm.moe.pokkedoll.warscore.utils.ItemUtil
import hm.moe.pokkedoll.warscore.{Callback, Test, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{ClickType, InventoryClickEvent, InventoryCloseEvent, InventoryType}
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
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

  val WEAPON_TYPES: Array[String] = Array(MAIN, SUB, MELEE, ITEM)

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

  def openWeaponStorageUI(player: HumanEntity, page: Int = 1): Unit = {
    if (WarsCoreAPI.getWPlayer(player.asInstanceOf[Player]).game.isDefined) return
    val inv = Bukkit.createInventory(null, 54, WEAPON_CHEST_UI_TITLE)
    (0 to 8).filterNot(f => f == 4 || f == 0).foreach(inv.setItem(_, PANEL))

    inv.setItem(0, new ItemStack(Material.BARRIER))

    val p = pageIcon(page)
    val baseSlot = (page - 1) * 45

    db.getPagedWeaponStorage(player.getUniqueId.toString, baseSlot, new Callback[mutable.Buffer[(Int, Array[Byte], Int)]] {
      override def success(value: mutable.Buffer[(Int, Array[Byte], Int)]): Unit = {
        inv.setItem(1, BACK_MAIN_UI)
        inv.setItem(4, p)
        value.foreach(f => {
          // println(f)
          val i = if (f._2 == null) new ItemStack(Material.AIR) else ItemStack.deserializeBytes(f._2)
          if (f._3 != 0) {
            i.addUnsafeEnchantment(Enchantment.BINDING_CURSE, 10)
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
  @Deprecated
  val weaponUsingKey = new NamespacedKey(WarsCore.instance, "weapon-using")

  @Deprecated
  private val usedSlotKey = new NamespacedKey(WarsCore.instance, "weapon-used-slot")
  @Deprecated
  private val usedTypeKey = new NamespacedKey(WarsCore.instance, "weapon-used-type")

  private val weaponKey = new NamespacedKey(WarsCore.instance, "weapon-key")

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
          val key = weapons(f)
          val i = ItemUtil.getItem(key).getOrElse(EMPTY).clone()
          val m = i.getItemMeta
          m.getPersistentDataContainer.set(weaponKey, PersistentDataType.STRING, key)
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
        } else if(i != null && i.getType != Material.AIR && i.getType != PANEL.getType) {
          val meta = i.getItemMeta
          val per = meta.getPersistentDataContainer
          if(per.has(weaponKey, PersistentDataType.STRING)) {
            db.setWeapon(player.getUniqueId.toString, t = per.get(weaponTypeKey, PersistentDataType.STRING), name = per.get(weaponKey, PersistentDataType.STRING))
            openMainUI(player)
          }
        }
      case _ =>
    }
  }

  def onClickWeaponStorageUI(e: InventoryClickEvent): Unit = {
    e.getClickedInventory.getType match {
      case InventoryType.PLAYER if e.getClick == ClickType.SHIFT_LEFT =>
        e.getWhoClicked.sendMessage(e.getWhoClicked.getOpenInventory.getType.toString)
      case InventoryType.CHEST =>
        val i = e.getCurrentItem
        if (e.getSlot >= 0 && e.getSlot < 9) {
          e.setCancelled(true)
          if (e.getSlot == 0) {
            e.getWhoClicked.closeInventory()
          } else if (e.getSlot == 4) {
            val pageIcon = e.getClickedInventory.getItem(4)
            val page = pageIcon.getItemMeta.getPersistentDataContainer.get(UI_PAGE_KEY, PersistentDataType.INTEGER)
            if (e.getClick == ClickType.RIGHT) {
              openWeaponStorageUI(e.getWhoClicked, page = page + 1)
            } else if (e.getClick == ClickType.LEFT) {
              if (page != 1) openWeaponStorageUI(e.getWhoClicked, page = page - 1)
            }
          } else if (e.getSlot == 1) {
            openMainUI(e.getWhoClicked)
          }
        } else if (i != null && (i.getType == Material.BARRIER || i.containsEnchantment(Enchantment.BINDING_CURSE))) {
          e.setCancelled(true)
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
      // println(s"page is $page $baseSlot")
      val mappedInv = (0 until 45).map(f => (f, inv.getItem(9 + f)))
      val groupedInv = mappedInv.groupBy(f => f._2 == null || f._2.getType == Material.AIR)
      db.setPagedWeaponStorage(player.getUniqueId.toString, baseSlot, groupedInv)
    }
  }

  class MySet(val slot: Int, val title: String, val main: ItemStack, val sub: ItemStack, val melee: ItemStack, val item: ItemStack)

  val MY_SET_TITLE = "MySet Settings"

  /**
   * NoneはAIRにしておこう
   *
   * @param player
   */
  def openMySetUI(player: HumanEntity): Unit = {
    val test = new Test("openMySetUI")
    val inv = Bukkit.createInventory(null, 18, MY_SET_TITLE)
    inv.setItem(1, BACK_MAIN_UI)
    player.openInventory(inv)
    new BukkitRunnable {
      override def run(): Unit = {
        val storage = db.getWeaponStorage(player.getUniqueId.toString)
        val myset = db.getMySet(player.getUniqueId.toString)
        myset.foreach(f => {
          val icon = new ItemStack(Material.LIME_DYE)
          val meta = icon.getItemMeta
          meta.setDisplayName(ChatColor.GREEN + s"マイセット ${f.slot + 1}")
          val conv = (item: ItemStack) => {
            if (item.getType == Material.AIR) {
              ChatColor.WHITE + "なし"
            } else {
              val name = WarsCoreAPI.getItemStackName(item)
              if (!storage.contains(item)) {
                icon.setType(Material.RED_DYE)
                meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + s"マイセット ${f.slot + 1}")
                ChatColor.RED + "⚠ " + ChatColor.stripColor(name)
              } else {
                name
              }
            }
          }
          meta.setLore(java.util.Arrays.asList(
            ChatColor.GRAY + "[" + ChatColor.DARK_AQUA + "左クリック" + ChatColor.GRAY + "]" + ChatColor.RED + "マイセットを装備する",
            ChatColor.GRAY + "[" + ChatColor.DARK_AQUA + "右クリック" + ChatColor.GRAY + "]" + ChatColor.RED + "装備をマイセットに登録",
            s"メイン: ${conv(f.main)}",
            s"サブ: ${conv(f.sub)}",
            s"近接: ${conv(f.melee)}",
            s"アイテム: ${conv(f.item)}"
          ))
          icon.setItemMeta(meta)
          inv.setItem(9 + f.slot, icon)
        })
        (0 to 8).filterNot(pred => myset.map(_.slot).contains(pred)).foreach(f => {
          inv.setItem(9 + f, {
            val i = new ItemStack(Material.GRAY_DYE)
            val m = i.getItemMeta
            m.setDisplayName("設定されていない！")
            m.setLore(java.util.Arrays.asList(
              ChatColor.GRAY + "[" + ChatColor.DARK_AQUA + "右クリック" + ChatColor.GRAY + "]" + ChatColor.RED + "装備をマイセットに登録"
            ))
            i.setItemMeta(m)
            i
          })
        })
      }
    }.runTask(WarsCore.instance)
    test.log(1)
  }

  private def checkedSerialize(i: ItemStack): Array[Byte] = if (i.getType == Material.BARRIER) null else i.serializeAsBytes()

  def onClickMySetUI(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    e.getClickedInventory.getType match {
      case InventoryType.CHEST =>
        val slot = e.getSlot
        val item = e.getCurrentItem
        val player = e.getWhoClicked
        if (slot == 1) {
          openMainUI(player)
        } else if (e.getClick == ClickType.RIGHT) {
          if (slot > 8 && item != null) {
            db.setMySet(
              player.getUniqueId.toString,
              slot - 9,
              new Callback[Unit] {
                override def success(value: Unit): Unit = {
                  openMySetUI(player)
                }

                override def failure(error: Exception): Unit = {
                  player.sendMessage("失敗！！！！")
                }
              })
          }
        } else if (e.getClick == ClickType.LEFT) {
          if (slot > 8 && item != null && item.getType == Material.LIME_DYE) {
            db.applyMySet(player.getUniqueId.toString, slot - 9, new Callback[Unit] {
              override def success(value: Unit): Unit = {
                openMainUI(player)
              }

              override def failure(error: Exception): Unit = {
                player.sendMessage("失敗の音")
              }
            })
          }
        }
      case _ =>
    }
  }
}
