package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import hm.moe.pokkedoll.warscore.utils.RankManager.getNextExp
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.{DisplaySlot, Scoreboard}

/**
 * @param player
 */
@Deprecated
class Sidebar(val player: Player) {
  val scoreboard: Scoreboard = Bukkit.getScoreboardManager.getNewScoreboard

  private val chatColor = ChatColor.translateAlternateColorCodes('&', _)

  def show(): Unit = {
    if(scoreboard.getObjective(DisplaySlot.SIDEBAR) != null) scoreboard.getObjective(DisplaySlot.SIDEBAR).unregister()
    val obj = scoreboard.registerNewObjective("sidebar", "dummy", "Wars")
    val wp = WarsCoreAPI.getWPlayer(player)
    val text = List(
      obj.getScore(chatColor(s"&9Rank: &b${wp.rank}")),
      obj.getScore(chatColor(s"&9EXP: &a${wp.exp} &7/ &a${getNextExp(wp.rank)}")),
      obj.getScore(" "),
      obj.getScore(chatColor("&6/game&f: 試合に参加")),
      obj.getScore(chatColor("&6/spawn&f: スポーン地点に戻る")),
      obj.getScore(chatColor("&6/sf&f: ステータスを設定")),
      obj.getScore(chatColor("&6/pp&f: コマンド一覧を表示")),
      obj.getScore(chatColor("&6&m/vote&f: 投票ページを開く"))
    )
    text.reverse.indices.foreach(f => text(f).setScore(f))
  }
}
