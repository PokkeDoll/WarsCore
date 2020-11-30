package hm.moe.pokkedoll.warscore.utils

import java.io.File
import java.util.Optional

import hm.moe.pokkedoll.warscore.{Test, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.{Bukkit, Material, NamespacedKey}
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.event.inventory.{ClickType, InventoryAction, InventoryClickEvent, InventoryCloseEvent}
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.inventory.{Inventory, ItemFlag, ItemStack}
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

object TagUtil {
  private lazy val plugin = WarsCore.instance

  private lazy val db = WarsCore.instance.database

  var configFile: File = _
  var config: FileConfiguration = _

  /**
   * コンフィグから取得される、有効なタグのキャッシュ
   */
  var cache: Map[String, TagInfo] = _

  def reloadConfig(): Unit = {
    val test = new Test("TagUtil.reloadConfig()")
    if (configFile == null) {
      createConfig() match {
        case Success(_) =>
          plugin.getLogger.info("tag.ymlの読み込みに成功しました")
        case Failure(e) =>
          e.printStackTrace()
          plugin.getLogger.warning("tag.ymlの読み込みに失敗しました")
          return
      }
    }
    val cs = config.getKeys(false)
    cache = cs.asScala.map(f => (f, new TagInfo(f, ChatColor.translateAlternateColorCodes('&', config.getString(s"${f}.name", "null")), config.getInt(s"${f}.price")))).toMap
    test.log()
  }

  def createConfig(): Try[Unit] = {
    configFile = new File(plugin.getDataFolder, "tag.yml")
    if (!configFile.exists()) {
      configFile.getParentFile.mkdirs()
      plugin.saveResource("tag.yml", false)
    }
    config = new YamlConfiguration
    Try(config.load(configFile))
  }

  def saveConfig(): Unit = {
    config.save(configFile)
  }

  /**
   * データベースのタグコンテナとタグキャッシュに含まれているか確かめる
   *
   * @return
   */
  def hasTag(player: Player, tagId: String): Boolean = {
    plugin.database.getTags(uuid = player.getUniqueId.toString).contains(tagId) && cache.contains(tagId)
  }


  /**
   * タグをセットする
   */
  def setTag(player: Player, tagId: String): Unit = {
    // タグ情報をデータベース側に送るのはプレイヤーがログアウトしたときかセーブしたときのみ！
    val tag = cache.getOrElse(tagId, "-")
    plugin.database.setTag(player.getUniqueId.toString, tagId)
    val board = WarsCoreAPI.scoreboards(player)
    val obj = board.getObjective("tag")
    obj.setDisplayName(s"$tag §f0")
    WarsCoreAPI.scoreboards.values
      .map(_.getObjective("tag"))
      .foreach(_.getScore(player.getName).setScore(0))
  }


  /**
   * タグを追加する(= 新たに獲得)
   */
  def addTag(player: Player, tagId: String): Unit = {

  }

  /**
   * アイテムスタックのタグ表示名からタグキーを取得する。<br>
   * 少し頭がいい
   *
   * @return
   */
  @Deprecated
  def getTagKeyFromItemStack(item: ItemStack): String = {
    if (!item.hasItemMeta || !item.getItemMeta.hasLore) return ""
    """タグ名:.*;""".r.findFirstMatchIn(String.join(";", item.getItemMeta.getLore)) match {
      case Some(result) =>
        cache.find(_._2.name == result.toString().replace(";", "")).map(_._1).getOrElse("NotFound")
      case None => ""
    }
  }

  val TAG_INVENTORY_TITLE: String = ChatColor.of("#63C7BE") + "TAG Inventory"

  private val next = {
    val i = new ItemStack(Material.PLAYER_HEAD)
    i.getItemMeta match {
      case meta: SkullMeta =>
        meta.setOwner("MHF_ArrowRight")
        meta.setDisplayName(ChatColor.RED + "次ページに進む")
        i.setItemMeta(meta)
      case _ =>
    }
    i
  }

  private val previous = {
    val i = new ItemStack(Material.PLAYER_HEAD)
    i.getItemMeta match {
      case meta: SkullMeta =>
        meta.setOwner("MHF_ArrowLeft")
        meta.setDisplayName(ChatColor.RED + "前ページへ戻る")
        i.setItemMeta(meta)
    }
    i
  }

  private val help = {
    val i = new ItemStack(Material.PLAYER_HEAD)
    i.getItemMeta match {
      case meta: SkullMeta =>
        meta.setOwner("MHF_Question")
        meta.setDisplayName("ヘルプ")
        meta.setLore(java.util.Arrays.asList(""))
        i.setItemMeta(meta)
    }
    i
  }

  private val close = {
    val i = new ItemStack(Material.BARRIER)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.RED + "インベントリを閉じる")
    i.setItemMeta(m)
    i
  }

  private val isOwn = {
    val i = new ItemStack(Material.LIME_DYE)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.GREEN + "自身の所持しているタグだけを表示する: ON")
    i.setItemMeta(m)
    i
  }

  private val isNotOwn = {
    val i = new ItemStack(Material.GRAY_DYE)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.RED + "自身が所持しているタグだけを表示する: OFF")
    i.setItemMeta(m)
    i
  }

  private val key = new NamespacedKey(WarsCore.instance, "wc-tag-key")
  private val value = new NamespacedKey(WarsCore.instance, "wc-tag-value")

  /**
   *
   * @param player
   * @param page   インベントリの1ページに入るタグ数は45個
   * @param filter allならすべて、ownは所持しているタグのみ
   */
  def openTagInventory(player: HumanEntity, page: Int = 1, filter: String = "all"): Unit = {
    val inv = Bukkit.createInventory(null, 54, TAG_INVENTORY_TITLE + s": $page")
    if (page != 1) inv.setItem(0, previous)
    inv.setItem(1, help)
    inv.setItem(7, close)
    inv.setItem(8, next)
    player.openInventory(inv)
    new BukkitRunnable {
      override def run(): Unit = {
        val tags = db.getTags(player.getUniqueId.toString)
        val currentTag = db.getTag(player.getUniqueId.toString)
        val currentTagItem = {
          if(currentTag.isEmpty) {
            val i = new ItemStack(Material.MAP)
            val m = i.getItemMeta
            m.setDisplayName(ChatColor.RED + "タグが設定されていません！")
            m.setLore(java.util.Arrays.asList(
              ChatColor.AQUA + "下のリストからタグを選んでください！"
            ))
            i.setItemMeta(m)
            i
          } else {
            val i = new ItemStack(Material.NAME_TAG)
            i.addEnchantment(Enchantment.ARROW_DAMAGE, 0)
            i.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            val m = i.getItemMeta
            m.setDisplayName(ChatColor.WHITE + "現在のタグ: " + cache(currentTag).name)
            m.setLore(java.util.Arrays.asList(
              ChatColor.LIGHT_PURPLE + "現在の称号です",
              ChatColor.RED + "左クリック: " + ChatColor.GRAY + "タグをリセット"
            ))
            i.setItemMeta(m)
            i
          }
        }
        inv.setItem(4, currentTagItem)
        if (filter == "own") {
          inv.setItem(3, isOwn)
          // 確実に0 ~ 最大45まで
          val holdTags = cache.filter(f => tags.contains(f._1)).slice((page - 1) * 45, ((page - 1) * 45) + 45).toIndexedSeq
          holdTags.indices.foreach(f => {
            val item = {
              val i = new ItemStack(Material.NAME_TAG)
              val m = i.getItemMeta
              m.setDisplayName(holdTags(f)._2.name)
              m.setLore(java.util.Arrays.asList(
                ChatColor.GREEN + "所持済み",
                ChatColor.RED + "左クリック: " + ChatColor.GRAY + "タグを切り替える"
              ))
              val c = m.getPersistentDataContainer
              c.set(key, PersistentDataType.STRING, holdTags(f)._1)
              c.set(value, PersistentDataType.STRING, holdTags(f)._2.name)
              i.setItemMeta(m)
              i
            }
            inv.setItem(9 + f, item)
          })
        } else {
          inv.setItem(3, isNotOwn)
          val sliceTags = cache.slice((page - 1) * 45, ((page - 1) * 45) + 45).toIndexedSeq
          sliceTags.indices.foreach(f => {
            val item = {
              val tag = sliceTags(f)
              if (tags.contains(tag._1)) {
                val i = new ItemStack(Material.NAME_TAG)
                val m = i.getItemMeta
                m.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(tag._2.name))
                m.setLore(java.util.Arrays.asList(
                  ChatColor.GREEN + "所持済み",
                  ChatColor.RED + "左クリック: " + ChatColor.GRAY + "タグを切り替える"
                ))
                i.setItemMeta(m)
                i
              } else {
                val i = new ItemStack(Material.PAPER)
                val m = i.getItemMeta
                m.setDisplayName(tag._2.name)
                m.setLore(java.util.Arrays.asList(
                  ChatColor.YELLOW + "" + ChatColor.UNDERLINE + s"${tag._2.price} コインが必要です！",
                  "\n",
                  ChatColor.RED + "左クリック + シフト: " + ChatColor.GRAY + "タグを購入する"
                ))
                val c = m.getPersistentDataContainer
                c.set(key, PersistentDataType.STRING, tag._1)
                c.set(value, PersistentDataType.STRING, tag._2.name)
                i.setItemMeta(m)
                i
              }
            }
            inv.setItem(9 + f, item)
          })
        }
      }
    }.runTaskLater(WarsCore.instance, 1L)
  }

  /**
   * タグインベントリがクリックされた時に呼び出されるメソッド
   * e.getCurrentItemはnullではないことがわかっている！！！！！
   * @param e
   */
  def onClick(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    //e.getWhoClicked.sendMessage(s"${e.getSlot} ${e.getCurrentItem.getType} ${e.getClick}")
    val player = e.getWhoClicked
    val page = e.getView.getTitle.replace(TAG_INVENTORY_TITLE + ": ", "").toInt
    val item = e.getCurrentItem
    if (e.getSlot == 3) {
      if(item.getType == Material.GRAY_DYE) {
        openTagInventory(player, filter = "own")
      } else if (item.getType == Material.LIME_DYE) {
        openTagInventory(player)
      }
    } else if (e.getSlot == 7) {
      player.closeInventory(InventoryCloseEvent.Reason.PLAYER)
    } else if (e.getSlot == 0 && item.getType == Material.PLAYER_HEAD) {
      openTagInventory(player, page = page - 1)
    } else if (e.getSlot == 8 && item.getType == Material.PLAYER_HEAD) {
      openTagInventory(player, page = page + 1)
    }
    if(e.getSlot == 4 && e.getCurrentItem.getType == Material.NAME_TAG && e.getClick == ClickType.LEFT) {
      e.getWhoClicked.sendMessage("WHO!")
    }
  }

  class TagInfo(val id: String, val name: String, val price: Int)

}
