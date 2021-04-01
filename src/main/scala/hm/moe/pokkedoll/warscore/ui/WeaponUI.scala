package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.db.WeaponDB
import hm.moe.pokkedoll.warscore.utils.{Item, ItemUtil}
import hm.moe.pokkedoll.warscore.{Registry, WarsCore}
import net.md_5.bungee.api.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{InventoryClickEvent, InventoryType}
import org.bukkit.inventory.{Inventory, ItemFlag, ItemStack}
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, Material}

import java.util

object WeaponUI {

  private lazy val db = WarsCore.instance.database

  /**
   * getWeapons()したときのキャッシュを保存する
   */
  var weaponCache = Map.empty[HumanEntity, (String, Int, Seq[Item])]

  /**
   * キャッシュを削除する
   *
   * @param player 削除するプレイヤー
   */
  def clearCache(player: HumanEntity): Unit = weaponCache -= player

  val sortTypeMap = Map(0 -> "獲得順", 1 -> "個数順", 2 -> "名前順")

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
    val inv = Bukkit.createInventory(null, 9, MAIN_UI_TITLE)
    inv.setContents(Array.fill(9)(PANEL))

    inv.setItem(8, CLOSE_INVENTORY_ICON)

    player.openInventory(inv)

    inv.setItem(0, {
      val i = new ItemStack(Material.BOOK)
      val m = i.getItemMeta
      m.setDisplayName(ChatColor.RED + "メニューに戻る")
      i.setItemMeta(m)
      i
    })

    inv.setItem(7, {
      val i = new ItemStack(Material.FLINT_AND_STEEL)
      val m = i.getItemMeta
      m.setDisplayName(ChatColor.RED + "アイテムを表示する(回覧用)")
      i.setItemMeta(m)
      i
    })

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
        inv.setItem(1, weaponIcon(ItemUtil.getItem(weapons._1).getOrElse(EMPTY)))
        inv.setItem(2, weaponIcon(ItemUtil.getItem(weapons._2).getOrElse(EMPTY)))
        inv.setItem(3, weaponIcon(ItemUtil.getItem(weapons._3).getOrElse(EMPTY)))
        inv.setItem(4, weaponIcon(ItemUtil.getItem(weapons._4).getOrElse(EMPTY)))
        inv.setItem(5, weaponIcon(ItemUtil.getItem(weapons._5).getOrElse(EMPTY)))
      }
    }.runTask(WarsCore.instance)
  }

  /**
   * InventoryTypeはCHESTであることがわかる
   *
   * @param e イベント
   */
  def onClickMainUI(player: HumanEntity, slot: Int): Unit = {
    val metadata = player.getMetadata("sortType")
    val sortType = if(metadata.isEmpty) {
      0
    } else {
      metadata.get(0).asInt()
    }
    slot match {
      case 0 => player.asInstanceOf[Player].performCommand("pp")
      case 1 => openSettingUI(player, 1, WeaponDB.PRIMARY, sortType)
      case 2 => openSettingUI(player, 1, WeaponDB.SECONDARY, sortType)
      case 3 => openSettingUI(player, 1, WeaponDB.MELEE, sortType)
      case 4 => openSettingUI(player, 1, WeaponDB.GRENADE, sortType)
      case 5 => openSettingUI(player, 1, WeaponDB.HEAD, sortType)
      //case 15 => openWeaponStorageUI(player)
      //case 16 => openMySetUI(player)
      case 7 => openStorageUI(player, 1, "item")
      case 8 => player.closeInventory()
      case _ =>
    }
  }

  private val pageIcon = (page: Int) => {
    val i = new ItemStack(Material.WRITABLE_BOOK)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.translateAlternateColorCodes('&', s"&e${if (page == 1) "-" else page - 1} &7← &a&l$page &r&7→ &e${page + 1}"))
    m.setLore(java.util.Arrays.asList("左クリック | - | 右クリック"))
    m.getPersistentDataContainer.set(Registry.PAGE_KEY, PersistentDataType.INTEGER, java.lang.Integer.valueOf(page))
    i.setItemMeta(m)
    i
  }

  val WEAPON_CHEST_UI_TITLE = "Weapon Chest"

  val SETTING_TITLE = "Weapon Settings"

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
  def onClickStorageUI(player: HumanEntity, i: ItemStack, slot: Int, isLeftClick: Boolean, isRightClick: Boolean): Unit = {
    slot match {
      case 0 => player.closeInventory()
      case 1 => openMainUI(player)
      case 4 =>
        val page = i.getItemMeta.getPersistentDataContainer.get(Registry.PAGE_KEY, PersistentDataType.INTEGER)
        if (isLeftClick) {
          if (page != 1) openStorageUI(player, page - 1)
        } else if (isRightClick) {
          openStorageUI(player, page + 1)
        }
      case _ =>
    }
  }

  private val sortByHaving = {
    val i = new ItemStack(Material.FEATHER)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.UNDERLINE + "" + ChatColor.GREEN + "獲得順で並び変える")
    i.setItemMeta(m)
    i
  }

  private val sortByName = {
    val i = new ItemStack(Material.NAME_TAG)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.UNDERLINE + "" + ChatColor.GREEN + "名前順で並び変える")
    i.setItemMeta(m)
    i
  }

  private val sortByAmount = {
    val i = new ItemStack(Material.EMERALD, 64)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.UNDERLINE + "" + ChatColor.GREEN + "所持数順で並び変える")
    i.setItemMeta(m)
    i
  }

  /**
   * 武器設定UIを開く
   *
   * @param player 対象のプレイヤー
   * @param page   ページ番号
   */
  def openSettingUI(player: HumanEntity, page: Int = 1, weaponType: String, sortType: Int = 0): Unit = {
    val inv = Bukkit.createInventory(null, 54, SETTING_TITLE)
    (0 to 8).filterNot(_ == 4).foreach(inv.setItem(_, PANEL))

    val p = pageIcon(page)

    val barrier = {
      val i = new ItemStack(Material.BARRIER)
      val meta = i.getItemMeta
      meta.getPersistentDataContainer.set(Registry.WEAPON_TYPE_KEY, PersistentDataType.STRING, weaponType)
      meta.getPersistentDataContainer.set(Registry.SORT_TYPE_KEY, PersistentDataType.INTEGER, Integer.valueOf(sortType))
      i.setItemMeta(meta)
      i
    }

    inv.setItem(0, barrier)

    //TODO レジストリを作成する

    // 獲得順
    inv.setItem(6, sortByHaving)
    // 名前順
    inv.setItem(7, sortByName)
    // 個数順
    inv.setItem(8, sortByAmount)

    val baseSlot = (page - 1) * 45
    new BukkitRunnable {
      override def run(): Unit = {
        val weapons = (weaponCache.get(player) match {
          case Some(tuple) if tuple._1 == weaponType && tuple._2 == sortType => tuple._3
          case _ =>
            val weapons = db.getWeapons(player.getUniqueId.toString, weaponType, sortType)
            weaponCache += (player -> (weaponType, sortType, weapons))
            weapons
        }).slice(baseSlot, baseSlot + 45)
        // println(s"size: ${weapons.size}")
        //
        inv.setItem(1, BACK_MAIN_UI)
        inv.setItem(4, p)
        weapons.indices.foreach(f => {
          val item = weapons(f)
          val i = ItemUtil.getItem(item.name).getOrElse(EMPTY).clone()
          val m = i.getItemMeta
          m.setDisplayName(m.getDisplayName + " × " + item.amount)
          m.getPersistentDataContainer.set(Registry.WEAPON_KEY, PersistentDataType.STRING, item.name)
          m.getPersistentDataContainer.set(Registry.WEAPON_TYPE_KEY, PersistentDataType.STRING, weaponType)
          i.setItemMeta(m)
          inv.setItem(9 + f, i)
        })
      }
    }.runTask(WarsCore.instance)
    player.openInventory(inv)
  }

  /**
   * openSettingUIに対して呼ばれる
   *
   */
  def onClickSettingUI(player: HumanEntity, inv: Inventory,  i: ItemStack, slot: Int): Unit = {
    inv.getType match {
      case InventoryType.CHEST =>
        // val pageItem = inv.getItem(4)
        // val usedSlotItem = inv.getItem(0)
        val index0 = inv.getItem(0).getItemMeta.getPersistentDataContainer
        val weaponType = index0.get(Registry.WEAPON_TYPE_KEY, PersistentDataType.STRING)
        val sortType = index0.get(Registry.SORT_TYPE_KEY, PersistentDataType.INTEGER)
        slot match {
          // バリアブロック。インベントリを閉じる
          case 0 =>
            player.closeInventory()
          // 額縁。メインメニューに戻る
          case 1 =>
            openMainUI(player)
          case 6 if sortType != 0 =>
            openSettingUI(player, 1, weaponType)
            player.setMetadata("sortType", new FixedMetadataValue(WarsCore.instance, 0))
          case 7 if sortType != 1 =>
            openSettingUI(player, 1, weaponType, 1)
            player.setMetadata("sortType", new FixedMetadataValue(WarsCore.instance, 1))
          case 8 if sortType != 2 =>
            openSettingUI(player, 1, weaponType, 2)
            player.setMetadata("sortType", new FixedMetadataValue(WarsCore.instance, 2))
          case _ if i != null && i.getType != Material.AIR && i.getType != PANEL.getType =>
            val meta = i.getItemMeta
            val per = meta.getPersistentDataContainer
            if (per.has(Registry.WEAPON_KEY, PersistentDataType.STRING)) {
              val t = per.get(Registry.WEAPON_TYPE_KEY, PersistentDataType.STRING)
              val name = per.get(Registry.WEAPON_KEY, PersistentDataType.STRING)
              db.setWeapon(player.getUniqueId.toString, weaponType = t, name = name)
              if (t == "head") {
                // 帽子だけの特殊な設定
                ItemUtil.getItem(name).foreach(head => player.getInventory.setHelmet(head))
              }
              openMainUI(player)
            }
          case _ =>
        }
      case _ =>
    }
  }
}
