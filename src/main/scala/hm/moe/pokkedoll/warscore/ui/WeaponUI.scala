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
    inv.setItem(14, new ItemStack(Material.CRAFTING_TABLE))
    inv.setItem(16, new ItemStack(Material.BARRIER))

    player.openInventory(inv)
    db.getWeapon(player.getUniqueId.toString, new Callback[mutable.Buffer[(Array[Byte], Int)]] {
      override def success(value: mutable.Buffer[(Array[Byte], Int)]): Unit = {
        val main = value.find(p => p._2 == 1) match {
          case Some(f) => ItemStack.deserializeBytes(f._1)
          case None => EMPTY
        }
        val sub = value.find(p => p._2 == 2) match {
          case Some(f) => ItemStack.deserializeBytes(f._1)
          case None => EMPTY
        }
        val melee = value.find(p => p._2 == 3) match {
          case Some(f) => ItemStack.deserializeBytes(f._1)
          case None => EMPTY
        }
        val item = value.find(p => p._2 == 4) match {
          case Some(f) => ItemStack.deserializeBytes(f._1)
          case None => EMPTY
        }

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
      case 14 => openMySetUI(player)
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
    (0 to 8).filterNot(f => f == 4 || f == 0).foreach(inv.setItem(_, PANEL))

    inv.setItem(0, new ItemStack(Material.BARRIER))

    val p = pageIcon(page)
    val baseSlot = (page - 1) * 45

    db.getPagedWeaponStorage(player.getUniqueId.toString, baseSlot, new Callback[mutable.Buffer[(Int, Array[Byte], Int)]] {
      override def success(value: mutable.Buffer[(Int, Array[Byte], Int)]): Unit = {
        inv.setItem(1, BACK_MAIN_UI)
        inv.setItem(4, p)
        value.foreach(f => {
          println(f)
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
  val weaponUsingKey = new NamespacedKey(WarsCore.instance, "weapon-using")

  private val usedSlotKey = new NamespacedKey(WarsCore.instance, "weapon-used-slot")

  private val usedTypeKey = new NamespacedKey(WarsCore.instance, "weapon-used-type")

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
        inv.setItem(1, BACK_MAIN_UI)
        inv.setItem(4, p)
        val barrier = new ItemStack(Material.BARRIER)
        val mm = barrier.getItemMeta
        mm.getPersistentDataContainer.set(usedTypeKey, PersistentDataType.INTEGER, java.lang.Integer.valueOf(weaponType + 1))
        value.foreach(f => {
          val i = if (f._2 == null) PANEL else ItemStack.deserializeBytes(f._2)
          if (i.getType != Material.AIR && i.hasItemMeta) {
            val m = i.getItemMeta
            if (m.hasLore && m.getLore.stream().anyMatch(pred => pred.contains(WEAPON_TYPES(weaponType)))) {
              if (f._3 != 0) {
                i.addUnsafeEnchantment(Enchantment.BINDING_CURSE, 10)
                mm.getPersistentDataContainer.set(usedSlotKey, PersistentDataType.INTEGER, java.lang.Integer.valueOf(f._1))
              }
              inv.setItem(9 + f._1 - baseSlot, i)
            } else {
              inv.setItem(9 + f._1 - baseSlot, PANEL)
            }
          } else {
            inv.setItem(9 + f._1 - baseSlot, PANEL)
          }
        })
        barrier.setItemMeta(mm)
        inv.setItem(0, barrier)
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

  def onClickSettingUI(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    val player = e.getWhoClicked
    val inv = e.getClickedInventory
    inv.getType match {
      case InventoryType.CHEST =>
        val i = e.getCurrentItem
        val pageItem = inv.getItem(4)
        val usedSlotItem = inv.getItem(0)
        if (e.getSlot == 0) {
          player.closeInventory()
        } else if (e.getSlot == 1) {
          openMainUI(player)
        } else if (pageItem != null && usedSlotItem != null && i != null && i.getType != Material.AIR && i.getType != PANEL.getType && e.getSlot > 8 && !i.containsEnchantment(Enchantment.BINDING_CURSE)) {
          val page = pageItem.getItemMeta.getPersistentDataContainer.get(UI_PAGE_KEY, PersistentDataType.INTEGER)
          val usedSlot = usedSlotItem.getItemMeta.getPersistentDataContainer.get(usedSlotKey, PersistentDataType.INTEGER)
          val usedType = usedSlotItem.getItemMeta.getPersistentDataContainer.get(usedTypeKey, PersistentDataType.INTEGER)

          val baseSlot = (page - 1) * 45
          println(s"page = $page, usedSlot = $usedSlot, usedType = $usedType")
          db.setPagedWeapon(player.getUniqueId.toString, baseSlot + (e.getSlot - 9), baseSlot + usedSlot, usedType, new Callback[Unit] {
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

  class MySet(val slot: Int, val title: String, val main: Option[Array[Byte]], val sub: Option[Array[Byte]], val melee: Option[Array[Byte]], val item: Option[Array[Byte]])

  val MY_SET_TITLE = "MySet Settings"

  /**
   * NoneはAIRにしておこう
   *
   * @param player
   */
  def openMySetUI(player: HumanEntity): Unit = {
    val inv = Bukkit.createInventory(null, 18, MY_SET_TITLE)
    inv.setItem(1, BACK_MAIN_UI)
    db.getMySet(player.getUniqueId.toString, new Callback[mutable.Buffer[MySet]] {
      override def success(value: mutable.Buffer[MySet]): Unit = {
        value.foreach(f => {

          val icon = new ItemStack(Material.LIME_DYE, f.slot + 1)

          val m = icon.getItemMeta

          val main = f.main match {
            case Some(bytes) if bytes != null => ItemStack.deserializeBytes(bytes)
            case _ => new ItemStack(Material.AIR)
          }
          val sub = f.sub match {
            case Some(bytes) if bytes != null => ItemStack.deserializeBytes(bytes)
            case _ => new ItemStack(Material.AIR)
          }
          val melee = f.melee match {
            case Some(bytes) if bytes != null => ItemStack.deserializeBytes(bytes)
            case _ => new ItemStack(Material.AIR)
          }
          val item = f.item match {
            case Some(bytes) if bytes != null => ItemStack.deserializeBytes(bytes)
            case _ => new ItemStack(Material.AIR)
          }

          m.setDisplayName(f.title)
          m.setLore(java.util.Arrays.asList(
            ChatColor.RED + "(動作しない)右クリックで現在の装備をこのマイセットに登録",
            ChatColor.RED + "(動作しない)左クリックで現在の装備をセット",
            ChatColor.RED + "(動作しない)シフト + 左クリックでマイセットの名前をセットする",
            s"めいん: ${if (main.hasItemMeta && main.getItemMeta.hasDisplayName) main.getItemMeta.getDisplayName else "なし"}",
            s"さぶ: ${if (sub.hasItemMeta && sub.getItemMeta.hasDisplayName) sub.getItemMeta.getDisplayName else "なし"}",
            s"近接: ${if (melee.hasItemMeta && melee.getItemMeta.hasDisplayName) melee.getItemMeta.getDisplayName else "なし"}",
            s"アイテム: ${if (item.hasItemMeta && item.getItemMeta.hasDisplayName) item.getItemMeta.getDisplayName else "なし"}"
          ))

          icon.setItemMeta(m)
          inv.setItem(9 + f.slot, icon)
        })
        (0 to 8).filterNot(pred => value.map(_.slot).contains(pred)).foreach(f => {
          inv.setItem(9 + f, {
            val i = new ItemStack(Material.GRAY_DYE)
            val m = i.getItemMeta
            m.setDisplayName("設定されていない！")
            m.setLore(java.util.Arrays.asList(ChatColor.RED + "(動作しない)右クリックで現在の装備をこのマイセットに登録"))
            i.setItemMeta(m)
            i
          })
        })
      }

      override def failure(error: Exception): Unit = {
        error.printStackTrace()
        player.sendMessage("エラー！")
      }
    })

    player.openInventory(inv)
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
