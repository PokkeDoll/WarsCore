package hm.moe.pokkedoll.warscore.ui

import hm.moe.pokkedoll.warscore.WarsCoreAPI.{games, getWPlayer}
import hm.moe.pokkedoll.warscore.games.{Game, GameState}
import net.md_5.bungee.api.ChatColor
import org.bukkit.{Bukkit, Material}
import org.bukkit.entity.{HumanEntity, Player}
import org.bukkit.inventory.{ItemFlag, ItemStack}

object GameUI {

  val GAME_INVENTORY_TITLE: String = ChatColor.GREEN + "ゲーム一覧！"

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
    game.state match {
      case GameState.PLAY | GameState.WAIT | GameState.READY =>
        i.setType(Material.LIME_DYE)
        i.setAmount(if(game.members.isEmpty) 1 else game.members.size)
        m.setDisplayName(ChatColor.GREEN + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.GREEN + game.mapInfo.mapName,
          ChatColor.GREEN + (if(game.state == GameState.PLAY) "試合中！" else if (game.state == GameState.WAIT) s"${game.members.size} / ${game.maxMember} 待機中！" else s"${game.members.size} / ${game.maxMember} 準備中！"),
        ))
      case GameState.PLAY2 =>
        i.setType(Material.GRAY_DYE)
        m.setDisplayName(ChatColor.GRAY + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.GREEN + game.mapInfo.mapName,
          ChatColor.GREEN + (if(game.state == GameState.PLAY) "試合中！" else if (game.state == GameState.WAIT) s"${game.members.size} / ${game.maxMember} 待機中！" else s"${game.members.size} / ${game.maxMember} 準備中！"),
          ChatColor.RED + "もうすぐ試合が終わるので参加できません！"
        ))
      case GameState.INIT =>
        i.setType(Material.PINK_DYE)
        m.setDisplayName(ChatColor.LIGHT_PURPLE + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.WHITE + "初期化中..."
        ))
      case GameState.ERROR =>
        i.setType(Material.PINK_DYE)
        m.setDisplayName(ChatColor.LIGHT_PURPLE + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.RED + "マップの読込に失敗しました"
        ))
      case GameState.END =>
        i.setType(Material.GRAY_DYE)
        m.setDisplayName(ChatColor.GRAY + game.id)
        m.setLore(java.util.Arrays.asList(
          ChatColor.LIGHT_PURPLE + game.title,
          ChatColor.YELLOW + "試合が終了しました"
        ))
      case _ =>
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
}
