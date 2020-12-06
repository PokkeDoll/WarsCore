package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.WarsCoreAPI.{games, getWPlayer}
import org.bukkit.{Bukkit, ChatColor, Material}
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.inventory.{ItemFlag, ItemStack}

object GameUI {

  val GAME_INVENTORY_TITLE = "§aゲーム一覧！"

  private val openGameInventoryIcon: ItemStack = {
    val i = new ItemStack(Material.ARROW, 1)
    val m = i.getItemMeta
    m.setUnbreakable(true)
    i.setItemMeta(m)
    i
  }

  /**
   * ゲームインベントリを開く
   *
   * @since v1.4.1
   * @param player 対象のプレイヤー
   */
  def openMainUI(player: HumanEntity): Unit = {
    val inv = Bukkit.createInventory(null, 27, GAME_INVENTORY_TITLE)
    var slot = (0, 9, 18)
    inv.setItem(0, openGameInventoryIcon)
    inv.setItem(9, new ItemStack(Material.IRON_SWORD))
    games.foreach(f => {
      val weight = if (f._1.startsWith("tdm")) {
        slot = (slot._1 + 1, slot._2, slot._3)
        slot._1
      } else if (f._1.startsWith("dom")) {
        slot = (slot._1, slot._2 + 1, slot._3)
        slot._2
      } else slot._3
      val icon = ((damage => new ItemStack(Material.STONE, f._2.members.size + 1, damage)): Short => ItemStack) (if (f._2.state.join) 10 else 8)
      val meta = icon.getItemMeta
      meta.setDisplayName((if (f._1.contains("test")) ChatColor.RED else ChatColor.WHITE) + s"${f._1}")
      meta.setLore(java.util.Arrays.asList(s"§f${f._2.title}", s"§e${f._2.description}", s"§a${f._2.members.size} §7/ §a${f._2.maxMember} プレイ中", s"§a${f._2.state.title}"))
      icon.setItemMeta(meta)
      inv.setItem(weight, icon)
    })
    player.openInventory(inv)
  }

  /**
   * 統計インベントリを開く
   *
   * @since v1.4.1
   * @param player Player
   * @param target Target
   */
  def openStatsUI(player: Player, target: Player): Unit = {
    val inv = Bukkit.createInventory(null, 18, s"${target.getName}'s stats'")
    val wp = getWPlayer(target)
    val tdmIcon = new ItemStack(Material.IRON_SWORD)
    val tdmIconMeta = tdmIcon.getItemMeta
    tdmIconMeta.setDisplayName(ChatColor.YELLOW + "TDM")
    tdmIconMeta.setLore(java.util.Arrays.asList(
      ChatColor.GRAY + "プレイした記録: " + -1,
      ChatColor.GRAY + "勝利した回数: " + -1,
      ChatColor.GRAY + "敵を倒した回数: " + -1,
      ChatColor.GRAY + "死んだ回数: " + -1,
      ChatColor.GRAY + "与えたダメージ: " + -1,
      ChatColor.GRAY + "*",
      ChatColor.GRAY + "占領に成功した回数: " + -1,
      ChatColor.GRAY + "Kill MVPを取得した回数: " + -1,
      ChatColor.GRAY + "Damage MVPを取得した回数: " * -1
    ))
    tdmIconMeta.setUnbreakable(true)
    tdmIconMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
    tdmIcon.setItemMeta(tdmIconMeta)

    val tacticsIcon = new ItemStack(Material.REDSTONE, 1)
    val tacticsIconMeta = tacticsIcon.getItemMeta
    tacticsIconMeta.setDisplayName(ChatColor.DARK_AQUA + "Tactics")
    tacticsIconMeta.setLore(java.util.Arrays.asList(
      "",
      "",
      ""
    ))
  }
}
