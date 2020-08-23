package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.{Test, WarsCore, WarsCoreAPI}
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.{DisplaySlot, Objective, Scoreboard}

/**
 * ランクを大雑把に管理するオブジェクト
 *
 * @author Emorard
 */
object RankManager {

  private lazy val plugin = WarsCore.instance

  private lazy val chatColor = ChatColor.translateAlternateColorCodes('&', _)

  /**
   * 次に必要な経験値を返す
   * @param rank ランク
   * @return
   */
  def getNextExp(rank: Int): Int = (100 * Math.pow(1.05, rank)).toInt

  /**
   * 自身のスコアボードの横部分を更新する
   */
  @Deprecated
  def updateSidebar(player: Player, sb: Scoreboard): Unit = {
    val test = new Test()
    if(sb.getObjective(DisplaySlot.SIDEBAR)!=null) sb.getObjective(DisplaySlot.SIDEBAR).unregister()
    val obj = sb.registerNewObjective("sidebar", "dummy")
    obj.setDisplayName("ハローユーチューブ")
    obj.setDisplaySlot(DisplaySlot.SIDEBAR)
    plugin.database.getRankData(player.getUniqueId.toString) match {
      case Some(data) =>
        val rank = obj.getScore(ChatColor.BLUE + s"Rank: ${data._1}")
        rank.setScore(5)

        val exp = obj.getScore(ChatColor.BLUE + s"§9EXP: ${data._2} / (${getNextExp(data._1)})")
        exp.setScore(4)

        val etc = obj.getScore(ChatColor.BLUE + "§6etc.")
        etc.setScore(3)
      case None =>
        val rank = obj.getScore(ChatColor.BLUE + "Rank: -1")
        rank.setScore(5)

        val exp = obj.getScore(ChatColor.BLUE + "§9EXP: -1 / -1")
        exp.setScore(4)

        val etc = obj.getScore(ChatColor.BLUE + "§6etc.")
        etc.setScore(3)
    }
    test.log("RankManager.updateSidebar()")
  }

  /**
   * データを直接渡すため高速
   * @param sb
   * @param data
   */
  def updateSidebar(sb: Scoreboard, data: (Int, Int)): Unit = {
    val test = new Test()
    if(sb.getObjective(DisplaySlot.SIDEBAR)!=null) sb.getObjective(DisplaySlot.SIDEBAR).unregister()
    val obj = sb.registerNewObjective("sidebar", "dummy")
    obj.setDisplayName("ステータス")
    obj.setDisplaySlot(DisplaySlot.SIDEBAR)

    val scores = List(
      obj.getScore(chatColor(s"&9Rank: &b${data._1}")),
      obj.getScore(chatColor(s"&9EXP: &a${data._2} &7/ &a${getNextExp(data._1)}")),
      obj.getScore(" "),
      obj.getScore(chatColor("&6/game&f: 試合に参加")),
      obj.getScore(chatColor("&6/spawn&f: スポーン地点に戻る")),
      obj.getScore(chatColor("&6/sf&f: ステータスを設定")),
      obj.getScore(chatColor("&6/whelp&f: ヘルプを表示")),
    )
    var sc = scores.length
    scores.foreach(s => {
      s.setScore(sc)
      sc-=1
    })
    test.log("RankManager.updateSidebar(Player, Scoreboard, (Int, Int))")
  }
}
