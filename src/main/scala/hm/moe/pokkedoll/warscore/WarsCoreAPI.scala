package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.utils.ItemUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.{Bukkit, Location, Material, NamespacedKey}
import org.bukkit.entity.Player
import org.bukkit.inventory.{ItemFlag, ItemStack}

import scala.collection.mutable


/**
 * 便利なメソッドをまとめたオブジェクト
 *
 * @author Emorard
 */
object WarsCoreAPI {
  /** データベース */
  private val database = WarsCore.instance.database


  /** プレイヤーのキャッシュ */
  val wplayers = new mutable.HashMap[Player, WPlayer](50, 1.0)

  /**
   * APIにキャッシュされているインスタンスを返す。ないなら作る
   *
   * @param player Player
   * @return
   */
  def getWPlayer(player: Player): WPlayer = wplayers.getOrElseUpdate(player, {
      val wp = new WPlayer(player)
      database.loadWPlayer(wp, new Callback[WPlayer] {
        override def success(value: WPlayer): Unit = {
          // addScoreBoard(player)
          println(value.disconnect)
          if (value.disconnect) {
            value.disconnect = false
            database.setDisconnect(player.getUniqueId.toString, disconnect = false)
            // WarsCoreAPI.restoreLobbyInventory(player)
            // player.teleport(WarsCoreAPI.DEFAULT_SPAWN)
          }
        }
        override def failure(error: Exception): Unit = {
          error.printStackTrace()
          player.sendMessage(Component.text("データの読み込みに失敗しました").color(NamedTextColor.RED))
        }
      })
      wp
    })


  /**
   * プレイヤーの所持している武器名を取得する
   *
   * @param player Player
   * @return
   */
  def getAttackerWeaponName(player: Player): Option[String] = {
    val item = player.getInventory.getItemInMainHand
    if (item == null || !item.hasItemMeta) None
    else {
      val meta = item.getItemMeta
      Some(if(meta.hasDisplayName) meta.getDisplayName else item.getType.toString)
    }
  }

  def randomChance(chance: Double): Boolean = (chance / 100.0) > Math.random()

  /**
   * 数字から時間を日本語にして返す
   *
   * @param biggy 試合の残り時間。秒
   * @return 時間、分、秒のタプル
   */
  def splitToComponentTimes(biggy: BigDecimal): (Int, Int, Int) = {
    val long = biggy.longValue
    val hours = (long / 3600).toInt
    var remainder = (long - hours * 3600).toInt
    val mins = remainder / 60
    remainder = remainder - mins * 60
    val secs = remainder
    (hours, mins, secs)
  }

  def getLocation(string: String): Option[Location] = {
    val arr = string.split(",")
    try {
      val location = new Location(Bukkit.getWorld(arr(0)), arr(1).toDouble, arr(2).toDouble, arr(3).toDouble, arr(4).toFloat, arr(5).toFloat)
      Some(location)
    } catch {
      case e: Exception =>
        WarsCore.instance.getLogger.warning(s"${e.getMessage} at WarsCoreAPI.getLocation($string)")
        None
    }
  }


  /**
   * ロビーのインベントリを退避する
   *
   * @version v1.3.16
   */
  /*
  def changeWeaponInventory(wp: WPlayer): Unit = {
    val player = wp.player
    database.setVInv(player.getUniqueId.toString, player.getInventory.getStorageContents, new Callback[Unit] {
      override def success(value: Unit): Unit = {
        setActiveWeapons(player)
      }

      override def failure(error: Exception): Unit = {
        wp.sendMessage("ロビーインベントリの読み込みに失敗しました")
      }
    })
  }
  */

  def setActiveWeapons(player: Player): Unit = {
    val weapons = database.getActiveWeapon(player.getUniqueId.toString)
    val get = (key: String, default: String) => ItemUtil.getItem(key).getOrElse(ItemUtil.getItem(default).get)
    player.getInventory.setContents(
      Array(
        get(weapons.main, "ak-47"),
        get(weapons.sub, "m92"),
        get(weapons.melee, "knife"),
        get(weapons.item, "grenade"),
        new ItemStack(Material.AIR),
        new ItemStack(Material.AIR),
        new ItemStack(Material.AIR),
        new ItemStack(Material.AIR),
        new ItemStack(Material.CLOCK)
      ))
    player.getInventory.setHelmet(ItemUtil.getItem(weapons.head).getOrElse(new ItemStack(Material.AIR)))
  }
  def getItemStackName(itemStack: ItemStack): String = {
    if (itemStack.hasItemMeta && itemStack.getItemMeta.hasDisplayName) {
      itemStack.getItemMeta.getDisplayName
    } else {
      itemStack.getType.toString.replaceAll("_", " ")
    }
  }

  val weaponUnlockNameKey = new NamespacedKey(WarsCore.instance, "weapon-unlock-name")
  val weaponUnlockTypeKey = new NamespacedKey(WarsCore.instance, "weapon-unlock-type")

  def unlockWeapon(player: Player, t: String, weapon: String): Unit = {
    database.addWeapon(player.getUniqueId.toString, t, weapon, 1)
  }

  def parseInt(string: String): Int = {
    try {
      string.toInt
    } catch {
      case _: NumberFormatException =>
        -1
    }
  }

  def getNamedItemStack(material: Material, name: String): ItemStack = {
    getNamedItemStack(material, name, java.util.Collections.emptyList())
  }

  def getNamedItemStack(material: Material, name: String, lore: java.util.List[String]): ItemStack = {
    val i = new ItemStack(material)
    i.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
    val m = i.getItemMeta
    m.setDisplayName(name)
    m.setLore(lore)
    i.setItemMeta(m)
    i
  }


}
