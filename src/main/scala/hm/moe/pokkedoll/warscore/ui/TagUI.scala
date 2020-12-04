package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.{Callback, WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.ui.WeaponUI.{PANEL, UI_PAGE_KEY, WEAPON_CHEST_UI_TITLE, pageIcon}
import hm.moe.pokkedoll.warscore.utils.TagUtil
import hm.moe.pokkedoll.warscore.utils.TagUtil.{cache, key, value}
import net.md_5.bungee.api.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.{Bukkit, Material, NamespacedKey}
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

import scala.collection.mutable

object TagUI {

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

  val UI_TITLE = ""

  def openUI(player: Player, page: Int = 1, holding: Boolean = false): Unit = {
    val inv = Bukkit.createInventory(null, 54, WEAPON_CHEST_UI_TITLE)
    (0 to 8).filterNot(_ == 4).foreach(inv.setItem(_, WarsCoreAPI.UI.PANEL))
    val p = WarsCoreAPI.UI.PAGE_ICON(page)
    val baseSlot = (page - 1) * 45
    WarsCore.instance.database.getTags(player.getUniqueId.toString, new Callback[mutable.Buffer[(String, Boolean)]] {
      override def success(value: mutable.Buffer[(String, Boolean)]): Unit = {
        inv.setItem(4, p)
        val current = value.find(_._2).getOrElse(("", true))
        val currentItem = {
          val i = new ItemStack(Material.NAME_TAG)
          i.addEnchantment(Enchantment.ARROW_DAMAGE, 0)
          val m = i.getItemMeta
          m.setDisplayName(TagUtil.getTag(current._1).name)
          m.setLore(java.util.Arrays.asList("あ"))
          i.setItemMeta(m)
          i
        }
        inv.setItem(5, currentItem)
        // 自身が所持しているタグのみ表示
        if(holding) {
          inv.setItem(3, HOLDING_ONLY)
          val holdTags = TagUtil.cache
            .filter(f => value.map(f => f._1).contains(f._1))
            .slice((page - 1) * 45, ((page - 1) * 45) + 45)
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
          val sliceTags = cache.slice((page - 1) * 45, ((page - 1) * 45) + 45).toIndexedSeq
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
                i.setItemMeta(m)
                i
              case None =>
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

}
