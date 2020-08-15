package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.ChatColor
import org.bukkit.entity.Player

/**
 * ランクを大雑把に管理するオブジェクト
 *
 * @author Emorard
 */
object RankManager {

  private lazy val plugin = WarsCore.instance

  /**
   * 次に必要な経験値を返す
   * @param rank ランク
   * @return
   */
  def getNextExp(rank: Int): Int = (100 * Math.pow(1.05, rank)).toInt

  /**
   * 自身のスコアボードの横部分を更新する
   */
  def updateScoreboard(player: Player): Unit = {
    val sb = WarsCoreAPI.scoreboards(player)
    plugin.database.getRankData(player.getUniqueId.toString) match {
      case Some(data) =>
        sb.getEntries.forEach(f => {
          if(f.startsWith(ChatColor.BLUE + "Rank:") && f != ChatColor.BLUE + s"Rank: ${data._1}") {
            sb.getEntries.remove(f)
            sb.getObjective("status").getScore(ChatColor.BLUE + s"Rank: ${data._1}").setScore(5)
          } else if (f.startsWith(ChatColor.BLUE + "EXP: ")) {
            sb.getEntries.remove(f)
            sb.getObjective("status").getScore(ChatColor.BLUE + s"EXP: ${data._2} / ${getNextExp(data._1)}")
          }
        })
    }
  }
}
