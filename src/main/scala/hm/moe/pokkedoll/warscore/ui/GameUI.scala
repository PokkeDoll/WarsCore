package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.WarsCoreAPI.{games, getWPlayer}
import hm.moe.pokkedoll.warscore.games.{Game, GameState}
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

  private def createIcon(game: Game): ItemStack = {
    val i = new ItemStack(Material.STONE)
    val m = i.getItemMeta
    if(game.state == GameState.PLAY || game.state == GameState.WAIT || game.state == GameState.READY) {
      i.setType(Material.LIME_DYE)
      m.setDisplayName(ChatColor.GREEN + game.id)
      m.setLore(java.util.Arrays.asList(
        ChatColor.LIGHT_PURPLE + game.title,
        ChatColor.GREEN + game.mapInfo.mapName,
        ChatColor.GREEN + (if(game.state == GameState.PLAY) "試合中！" else if (game.state == GameState.WAIT) s"${game.members.size} / ${game.maxMember} 待機中！" else s"${game.members.size} / ${game.maxMember} 準備中！"),
      ))
    } else if (game.state == GameState.INIT) {
      i.setType(Material.PINK_DYE)
      m.setDisplayName(ChatColor.LIGHT_PURPLE + game.id)
      m.setLore(java.util.Arrays.asList(
        ChatColor.LIGHT_PURPLE + game.title,
        ChatColor.WHITE + "初期化中..."
      ))
    } else {
      i.setType(Material.GRAY_DYE)
      m.setDisplayName(ChatColor.GRAY + game.id)
      m.setLore(java.util.Arrays.asList(
        ChatColor.LIGHT_PURPLE + game.title,
        ChatColor.RED + "クリックして部屋を作成します"
      ))
    }
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
    val inv = Bukkit.createInventory(null, 36, GAME_INVENTORY_TITLE)
    inv.setItem(0, openGameInventoryIcon)
    inv.setItem(9, new ItemStack(Material.IRON_SWORD))
    val g = games.groupBy(f => f._2.title)
    var slot = 0
    g.foreach(f => {
      var s = slot
      f._2.foreach(ff => {
        inv.setItem(s, createIcon(ff._2))
        s += 1
      })
      slot += 9
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
