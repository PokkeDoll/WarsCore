package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.{Test, WarsCore}
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.inventory.{Inventory, ItemStack}
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, ChatColor, Material, Sound}

import scala.util.Try

/**
 * エンダーチェストを管理する
 *
 * @author Emorard
 */
@Deprecated
object EnderChestManager {

  private lazy val db = WarsCore.instance.database

  def parseChestId(name: String): Int = {
    """[0-9]*$""".r.findFirstMatchIn(ChatColor.stripColor(name)) match {
      case Some(result) =>
        Try(result.toString().toInt).getOrElse(0)
      case None => 0
    }
  }

  private val PRESENT: ItemStack = {
    val i = new ItemStack(Material.LEGACY_SKULL_ITEM, 1, 3)
    val m = i.getItemMeta.asInstanceOf[SkullMeta]
    m.setDisplayName(ChatColor.LIGHT_PURPLE + "プレゼントボックスを開く" + ChatColor.GRAY + ": " + ChatColor.LIGHT_PURPLE + "0")
    m.setOwner("MHF_Present2")
    i.setItemMeta(m)
    i
  }

  private val NONE: ItemStack = {
    val i = new ItemStack(Material.LEGACY_STAINED_GLASS_PANE, 1, 14)
    val m = i.getItemMeta
    m.setDisplayName(" ")
    i.setItemMeta(m)
    i
  }

  val ENDER_CHEST_MENU_TITLE: String = ChatColor.LIGHT_PURPLE + "EnderChest Menu"

  val ENDER_CHEST_MENU: Inventory = {
    val inv = Bukkit.createInventory(null, 36, ChatColor.LIGHT_PURPLE + "EnderChest Menu")
    lazy val createIcon = (slot => {
      val i = new ItemStack(Material.ENDER_CHEST, slot + 1)
      val m = i.getItemMeta
      m.setDisplayName(ChatColor.YELLOW + "Ender Chest" + ChatColor.GRAY + ": " + ChatColor.LIGHT_PURPLE + (slot + 1))
      m.setLore(java.util.Arrays.asList(ChatColor.GRAY + "エンダーチェストを開きます"))
      i.setItemMeta(m)
      i
    }): Int => ItemStack
    (0 to 26).foreach(f => inv.setItem(f, createIcon(f)))
    (27 to 35).foreach(f => inv.setItem(f, if (f == 27) PRESENT else NONE))
    inv
  }

  /**
   * エンダーチェストのメニューを表示する
   *
   * @param player プレイヤー
   */
  def openEnderChestMenu(player: Player): Unit = {
    player.playSound(player.getLocation, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f)
    player.openInventory(ENDER_CHEST_MENU)
  }

  /**
   * エンダーチェストの中身を表示する
   * Takuyaよりも高速に動作するように
   *
   * @param player InventoryClickEventからやってくる
   * @param id     開くエンダーチェストのID, プレゼントボックスは0
   */
  def openEnderChest(player: HumanEntity, id: Int): Unit = {
    val test = new Test("openEnderChest")
    val inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + player.getName + "'s Chest " + id)
    new BukkitRunnable {
      override def run(): Unit = {
        db.getStorage(id, player.getUniqueId.toString) match {
          case Some(str) =>
            val yaml = new YamlConfiguration()
            try {
              yaml.loadFromString(str)
              (0 to 53).foreach(i => {
                inv.setItem(i, yaml.getItemStack(i.toString, new ItemStack(Material.AIR)))
              })
            } catch {
              case e: Exception =>
                player.sendMessage(ChatColor.RED + s"エラーが発生しました。管理者に報告してください (${e.getMessage})")
            }
          case None =>
            player.sendMessage(ChatColor.RED + "データを作成しています...")
        }
      }
    }.runTaskAsynchronously(WarsCore.instance)
    player.openInventory(inv)
    test.log()
  }

  def closeEnderChest(player: HumanEntity, id: Int, content: Array[ItemStack]): Unit = {
    db.setStorage(id, player.getUniqueId.toString, i2s(content))
  }

  def i2s(items: Array[ItemStack]): String = {
    if (items == null || items.length != 54) " " else {
      val yaml = new YamlConfiguration
      (0 to 53).foreach(i => {
        yaml.set(i.toString, items(i))
      })
      yaml.saveToString()
    }
  }

  def s2i(string: String): Option[Array[ItemStack]] = {
    val items = new Array[ItemStack](54)
    if (string == null || string == "" || string == " ") None else {
      val yaml = new YamlConfiguration
      try {
        yaml.loadFromString(string)
        (0 to 53).foreach(i => {
          items(i) = yaml.getItemStack(i.toString, new ItemStack(Material.AIR))
        })
        Some(items)
      } catch {
        case e: Exception => e.printStackTrace(); None
      }
    }
  }
}
