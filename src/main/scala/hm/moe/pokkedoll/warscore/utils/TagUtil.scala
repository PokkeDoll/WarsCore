package hm.moe.pokkedoll.warscore.utils

import java.io.File
import java.util.Optional

import hm.moe.pokkedoll.warscore.{Test, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.{Bukkit, Material, NamespacedKey}
import org.bukkit.configuration.file.{FileConfiguration, YamlConfiguration}
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.inventory.{Inventory, ItemStack}
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
  var cache: Map[String, String] = _

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
    cache = cs.asScala.map(f => (f, ChatColor.translateAlternateColorCodes('&', config.getString(f, "null")))).toMap
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
  def getTagKeyFromItemStack(item: ItemStack): String = {
    if (!item.hasItemMeta || !item.getItemMeta.hasLore) return ""
    """タグ名:.*;""".r.findFirstMatchIn(String.join(";", item.getItemMeta.getLore)) match {
      case Some(result) =>
        cache.find(_._2 == result.toString().replace(";", "")).map(_._1).getOrElse("NotFound")
      case None => ""
    }
  }

  val TAG_INVENTORY_TITLE: String = ChatColor.of("63C7BE") + "TAG Inventory"

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

  private val key = new NamespacedKey(WarsCore.instance, "wc-tag-key")
  private val value = new NamespacedKey(WarsCore.instance, "wc-tag-value")
  /**
   *
   * @param player
   * @param page インベントリの1ページに入るタグ数は45個
   * @param filter allならすべて、ownは所持しているタグのみ
   */
  def openTagInventory(player: HumanEntity, page: Int = 1, filter: String = "all"): Unit = {
    val inv = Bukkit.createInventory(null, 54, TAG_INVENTORY_TITLE)
    if (page != 1) inv.setItem(0, previous)
    inv.setItem(1, help)
    inv.setItem(8, next)
    new BukkitRunnable {
      override def run(): Unit = {
        val tags = db.getTags(player.getUniqueId.toString)
        if (filter == "own") {
          // 確実に0 ~ 最大45まで
          val holdTags = cache.filter(f => tags.contains(f._1)).slice((page - 1) * 45, ((page - 1) * 45) + 45).toIndexedSeq
          holdTags.indices.foreach(f => {
            val item = {
              val i = new ItemStack(Material.NAME_TAG)
              val m = i.getItemMeta
              m.setDisplayName(holdTags(f)._2)
              m.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "左クリックで切り替え"))
              val c = m.getPersistentDataContainer
              c.set(key, PersistentDataType.STRING, holdTags(f)._1)
              c.set(value, PersistentDataType.STRING, holdTags(f)._2)
              i.setItemMeta(m)
              i
            }
            inv.setItem(f, item)
          })
        }
      }
    }
  }
}
