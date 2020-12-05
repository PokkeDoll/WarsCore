package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.utils.TagUtil
import hm.moe.pokkedoll.warscore.utils.TagUtil.cache
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

  def openUI(player: Player, page: Int = 1, holding: Boolean = false): Unit = {
    val inv = Bukkit.createInventory(null, 54, UI_TITLE)
    (0 to 8).filterNot(_ == 4).foreach(inv.setItem(_, WarsCoreAPI.UI.PANEL))
    val p = WarsCoreAPI.UI.PAGE_ICON(page)
    val baseSlot = (page - 1) * 45
    player.openInventory(inv)
    WarsCore.instance.database.getTags(player.getUniqueId.toString, new Callback[mutable.Buffer[(String, Boolean)]] {
      override def success(value: mutable.Buffer[(String, Boolean)]): Unit = {
        inv.setItem(4, p)
        val current = value.find(_._2).getOrElse(("", true))
        val currentItem = {
          val i = new ItemStack(Material.NAME_TAG)
          val m = i.getItemMeta
          m.setDisplayName(TagUtil.getTag(current._1).name)
          m.setLore(java.util.Arrays.asList("あ"))
          i.setItemMeta(m)
          i
        }
        inv.setItem(5, currentItem)
        // 自身が所持しているタグのみ表示
        if (holding) {
          inv.setItem(3, HOLDING_ONLY)
          val holdTags = TagUtil.cache
            .filter(f => value.map(f => f._1).contains(f._1))
            .slice(baseSlot, baseSlot + 45)
            .toIndexedSeq
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
              c.set(tagKey, PersistentDataType.STRING, holdTags(f)._1)
              c.set(tagValue, PersistentDataType.STRING, holdTags(f)._2.name)
              i.setItemMeta(m)
              i
            }
            inv.setItem(9 + f, item)
          })
        } else {
          inv.setItem(3, ALL)
          val sliceTags = cache.slice(baseSlot, baseSlot + 45).toIndexedSeq
          sliceTags.indices.foreach(f => {
            val tag = sliceTags(f)
            val item = value.find(p => p._1 == tag._1) match {
              case Some(_) =>
                val i = new ItemStack(Material.NAME_TAG)
                val m = i.getItemMeta
                m.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(tag._2.name))
                m.setLore(java.util.Arrays.asList(
                  ChatColor.GREEN + "所持済み",
                  ChatColor.RED + "左クリック: " + ChatColor.GRAY + "タグを切り替える"
                ))
                val c = m.getPersistentDataContainer
                c.set(tagKey, PersistentDataType.STRING, tag._1)
                c.set(tagValue, PersistentDataType.STRING, tag._2.name)
                i.setItemMeta(m)
                i
              case None if !TagUtil.isLimited(tag._2) =>
                val i = new ItemStack(Material.PAPER)
                val m = i.getItemMeta
                m.setLore(java.util.Arrays.asList(
                  ChatColor.YELLOW + "" + ChatColor.UNDERLINE + s"${tag._2.price} コインが必要です！",
                  "\n",
                  ChatColor.RED + "左クリック + シフト: " + ChatColor.GRAY + "タグを購入する"
                ))
                val c = m.getPersistentDataContainer
                c.set(tagKey, PersistentDataType.STRING, tag._1)
                c.set(tagValue, PersistentDataType.STRING, tag._2.name)
                i.setItemMeta(m)
                i
              case None =>
                WarsCoreAPI.UI.PANEL
            }
            inv.setItem(9 + f, item)
          })
        }
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
