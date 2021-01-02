package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.WarsCore
import net.md_5.bungee.api.ChatColor
import org.bukkit.{Bukkit, Material, NamespacedKey, Registry, Sound}
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object SndCheckerUI {
  private lazy val plugin = WarsCore.instance

  val TITLE: String = ChatColor.of("#b0e0e6") + "" + ChatColor.BOLD + "SndChecker"

  lazy val contents: Seq[ItemStack] = {
    Sound.values().map(sound => {
      val i = new ItemStack(Material.JUKEBOX)
      val m = i.getItemMeta
      m.setDisplayName(sound.name())
      i.setItemMeta(m)
      i
    })
  }


  private val backIcon = {
    val i = new ItemStack(Material.ACACIA_SIGN)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.RED + "前のページへ戻る")
    i.setItemMeta(m)
    i
  }

  private val nextIcon = {
    val i = new ItemStack(Material.JUNGLE_SIGN)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.RED + "次のページへ進む")
    i.setItemMeta(m)
    i
  }

  private val pageKey = new NamespacedKey(plugin, "sndchecker-page")
  private val pitchKey = new NamespacedKey(plugin, "sndchecker-pitch")

  val infoIcon = (page: Int, pitch: Float) => {
    val i = new ItemStack(Material.BOOKSHELF)
    val m = i.getItemMeta
    m.setDisplayName(ChatColor.of("#dc143c") + "ページ番号: " + page)
    m.setLore(java.util.Arrays.asList(
      ChatColor.DARK_AQUA + "【本棚を右クリック】 " + ChatColor.YELLOW + "音程を上げる",
      ChatColor.DARK_AQUA + "【本棚を左クリック】 " + ChatColor.YELLOW + "音程を下げる",
      ChatColor.DARK_AQUA + "【クリック】 " + ChatColor.YELLOW + "SEを再生する",
      ChatColor.DARK_AQUA + "【シフト+クリック】 " + ChatColor.YELLOW + "SEを停止する",
      ChatColor.WHITE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-",
      ChatColor.GREEN + "音程: " + pitch,
    ))
    val data = m.getPersistentDataContainer
    data.set(pageKey, PersistentDataType.INTEGER, Integer.valueOf(page))
    data.set(pitchKey, PersistentDataType.FLOAT, java.lang.Float.valueOf(pitch))
    i.setItemMeta(m)
    i
  }

  private val panelIcon = {
    val i = new ItemStack(Material.GRAY_STAINED_GLASS_PANE)
    val m = i.getItemMeta
    m.setDisplayName(" ")
    i.setItemMeta(m)
    i
  }

  def openUI(player: Player, page: Int = 1, pitch: Float = 1f): Unit = {
    val baseSlot = (page - 1) * 45
    val inv = Bukkit.createInventory(null, 54, TITLE)
    inv.setContents((Seq(backIcon, panelIcon, panelIcon, panelIcon, infoIcon(page, pitch), panelIcon, panelIcon, panelIcon, nextIcon) ++ contents.slice(baseSlot, baseSlot + 45)).toArray)
    player.openInventory(inv)
  }

  def onClick(e: InventoryClickEvent): Unit = {
    e.setCancelled(true)
    val player = e.getWhoClicked.asInstanceOf[Player]
    val item = e.getCurrentItem
    val inv = e.getClickedInventory
    if (inv != null) {
      val data = ((item: ItemStack) => item.getItemMeta.getPersistentDataContainer) (inv.getItem(4))
      val page = data.get(pageKey, PersistentDataType.INTEGER)
      val pitch = data.get(pitchKey, PersistentDataType.FLOAT)
      // 前に戻る
      if (e.getSlot == 0) {
        if (page > 1) {
          openUI(player, page - 1, pitch)
        }
      } else if (e.getSlot == 8) {
        openUI(player, page + 1, pitch)
      } else if (e.getSlot == 4) {
        if (e.isLeftClick) {
          if (pitch != 0f) {
            openUI(player, page, pitch - 1f)
          }
        } else {
          if (pitch != 2) {
            openUI(player, page, pitch + 1f)
          }
        }
      } else if (item != null && item.getType == Material.JUKEBOX && item.hasItemMeta && item.getItemMeta.hasDisplayName) {
        val sound = Sound.valueOf(item.getItemMeta.getDisplayName)
        if(e.isShiftClick)
          player.stopSound(sound)
        else
          player.playSound(player.getLocation, sound, 1f, pitch)
      }
    }

  }
}
