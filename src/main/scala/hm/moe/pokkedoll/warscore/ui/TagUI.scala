package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.utils.TagUtil
import hm.moe.pokkedoll.warscore.utils.TagUtil.{UserTagInfo, cache}
import hm.moe.pokkedoll.warscore.{Callback, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.inventory.{ClickType, InventoryClickEvent, InventoryType}
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.{Bukkit, Material, NamespacedKey}

import scala.collection.mutable

object TagUI {
  /**
   * 実際はmaxPage + 1
   */
  private val maxPage = 4

  private val HOLDING_ONLY = {
    val i = new ItemStack(Material.LIME_DYE)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.GREEN + "自身の所持しているタグだけを表示する: ON")
    i.setItemMeta(m)
    i
  }

  private val ALL = {
    val i = new ItemStack(Material.GRAY_DYE)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.RED + "自身が所持しているタグだけを表示する: OFF")
    i.setItemMeta(m)
    i
  }

  private val tagKey = new NamespacedKey(WarsCore.instance, "tag-key")
  private val tagValue = new NamespacedKey(WarsCore.instance, "tag-value")

  val UI_TITLE: String = ChatColor.of("#63C7BE") + "TAG Inventory"

  /**
   * タグインベントリを開く
   * @param player 対象のプレイヤー
   * @param page ページ番号
   * @param holding 所持しているタグだけを表示するか
   */
  def openUI(player: Player, page: Int = 1, holding: Boolean = false): Unit = {
    val inv = Bukkit.createInventory(null, 54, UI_TITLE)
    (0 to 8).filterNot(_ == 4).foreach(inv.setItem(_, WarsCoreAPI.UI.PANEL))
    val p = WarsCoreAPI.UI.PAGE_ICON(page)
    val baseSlot = (page - 1) * 45
    player.openInventory(inv)
    WarsCore.instance.database.getTags(player.getUniqueId.toString, new Callback[Vector[UserTagInfo]] {
      override def success(value: Vector[UserTagInfo]): Unit = {
        inv.setItem(4, p)
        // 現在設定しているタグを取得する
        val current = value.find(_.use).map(_.tagId)
        val currentItem = {
          val i = new ItemStack(Material.NAME_TAG)
          val m = i.getItemMeta
          m.setDisplayName(
            current match {
              case Some(v) => ChatColor.GREEN + "現在のタグ: " + TagUtil.getTag(v).name
              case None => ChatColor.RED + "設定されていません"
            }
          )
          i.setItemMeta(m)
          i
        }
        inv.setItem(5, currentItem)

        val tags = ((map => if(holding) map.filter(_._2) else map): Map[TagUtil.TagInfo, Boolean] => Map[TagUtil.TagInfo, Boolean])(TagUtil.cache.map(f => (f._2, value.map(_.tagId).contains(f._1)))).slice(baseSlot, baseSlot + 45).toIndexedSeq
        inv.setItem(3, if(holding) HOLDING_ONLY else ALL)

        tags.indices.foreach(f => {
          val tag = tags(f)
          inv.setItem(f + 9, if(tag._2) {
            val i = new ItemStack(Material.NAME_TAG)
            val m = i.getItemMeta
            m.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(tag._1.name))
            m.setLore(java.util.Arrays.asList(
              ChatColor.GREEN + "所持済み",
              ChatColor.RED + "左クリック: " + ChatColor.GRAY + "タグを切り替える"
            ))
            val c = m.getPersistentDataContainer
            c.set(tagKey, PersistentDataType.STRING, tag._1.id)
            c.set(tagValue, PersistentDataType.STRING, tag._1.name)
            i.setItemMeta(m)
            i
          } else {
            val i = new ItemStack(Material.PAPER)
            val m = i.getItemMeta
            m.setLore(java.util.Arrays.asList(
              ChatColor.YELLOW + "" + ChatColor.UNDERLINE + s"${tag._1.price} コインが必要です！",
              "\n",
              ChatColor.RED + "左クリック + シフト: " + ChatColor.GRAY + "タグを購入する"
            ))
            val c = m.getPersistentDataContainer
            c.set(tagKey, PersistentDataType.STRING, tag._1.id)
            c.set(tagValue, PersistentDataType.STRING, tag._1.name)
            i.setItemMeta(m)
            i
          })
        })
      }

      override def failure(error: Exception): Unit = {
        player.sendMessage("エラー！")
      }
    })
  }

  def onClick(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    val item = e.getCurrentItem // Not Nullが保証
    val inv = e.getClickedInventory
    val player = e.getWhoClicked.asInstanceOf[Player]
    if (inv.getType != InventoryType.CHEST) return
    if(e.getSlot == 4) {
      val page = inv.getItem(4).getItemMeta.getPersistentDataContainer.get(WarsCoreAPI.UI.PAGE_KEY, PersistentDataType.INTEGER)
      if (e.getClick == ClickType.RIGHT) {
        if (page < maxPage) openUI(player, page + 1)
      } else if (e.getClick == ClickType.LEFT) {
        if (page != 1) openUI(player, page - 1)
      }
    } else if (e.getSlot == 3) {
      if(inv.getItem(3).getType == Material.GRAY_DYE) {
        openUI(player, holding = true)
      } else {
        openUI(player)
      }
    } else if(item.getType == Material.PAPER && e.getClick == ClickType.SHIFT_LEFT) {
      player.sendMessage("購入！購入！")
    } else if(item.getType == Material.NAME_TAG && e.getClick == ClickType.LEFT) {
      player.sendMessage("切り替え！")
    }
  }
}
