package hm.moe.pokkedoll.warscore.utils

import java.nio.charset.StandardCharsets
import java.util.UUID

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.{Bukkit, ChatColor, Material, Sound}
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.inventory.{Inventory, ItemStack}
import org.bukkit.scheduler.BukkitRunnable

import scala.collection.mutable

/**
 * エンダーチェストを管理する
 * @author Emorard
 */
object EnderChestManager {

  private lazy val db = WarsCore.instance.database

  private val cache = mutable.HashMap.empty[String, Array[Byte]]

  val ENDER_CHEST_MENU: Inventory = {
    val inv = Bukkit.createInventory(null, 9, ChatColor.LIGHT_PURPLE + "EnderChest Menu")
    lazy val createIcon = (slot => {
      val i = new ItemStack(Material.ENDER_CHEST, slot + 1)
      val m = i.getItemMeta
      m.setDisplayName(ChatColor.YELLOW + "Ender Chest" + ChatColor.GRAY + ": " + ChatColor.LIGHT_PURPLE + (slot + 1))
      i.setItemMeta(m)
      i
    }): Int => ItemStack
    (0 to 8).foreach(f => inv.setItem(f,createIcon(f)))
    inv
  }

  /**
   * エンダーチェストのメニューを表示する
   * @param player
   */
  def openEnderChestMenu(player: Player): Unit = {
    player.playSound(player.getLocation, Sound.BLOCK_ENDERCHEST_OPEN, 1f, 1f)
    player.openInventory(ENDER_CHEST_MENU)
  }

  /**
   * エンダーチェストの中身を表示する
   * Takuyaよりも高速に動作するように
   * @param player
   * @param id
   */
  def openEnderChest(player: HumanEntity, id: Int): Unit = {
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
            player.sendMessage(ChatColor.RED + "データは空です！")
        }
      }
    }.runTaskAsynchronously(WarsCore.instance)
    player.openInventory(inv)
  }

  def closeEnderChest(player: HumanEntity, id: Int, content: Array[ItemStack]): Unit = {
    db.setStorage(id, player.getUniqueId.toString, i2s(content))
  }



  def i2s(items: Array[ItemStack]): String = {
    if(items==null || items.length != 54) " " else {
      val yaml = new YamlConfiguration
      (0 to 53).foreach(i => {
        yaml.set(i.toString, items(i))
      })
      yaml.saveToString()
    }
  }

  def s2i(string: String): Option[Array[ItemStack]] = {
    val items = new Array[ItemStack](54)
    if(string==null || string=="" || string==" ") None else {
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
