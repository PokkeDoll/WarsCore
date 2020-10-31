package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.{Test, WPlayer, WarsCore, WarsCoreAPI}
import org.bukkit.{ChatColor, Sound, scheduler}
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
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
   *
   * @param rank ランク
   * @return
   */
  def getNextExp(rank: Int): Int = (100 * Math.pow(1.05, rank)).toInt

  def giveExp(wp: WPlayer, amount: Int) = new BukkitRunnable {
    override def run(): Unit = {
      val player = wp.player
      val uuid = player.getUniqueId.toString
      plugin.database.getRankData(uuid) match {
        // (rank, exp)
        case Some(data) =>
          val nexp = data._2 + amount
          val sb = wp.player.getScoreboard
          val obj = sb.getObjective(DisplaySlot.SIDEBAR)
          val ndata = if (getNextExp(data._1) <= nexp) {
            // レベルアップ！とサイドバーに表記
            // 現在は8...
            sb.resetScores(chatColor(s"&9Rank: &b${data._1}"))
            obj.getScore(chatColor("&9Rank &b&lRANK UP!!")).setScore(8)
            // 現在は8 - 1...
            sb.resetScores(chatColor(s"&9EXP: &a${data._2} &7/ &a${getNextExp(data._1)}"))
            obj.getScore(chatColor(s"&9EXP: &aRANK UP!")).setScore(7)

            player.playSound(player.getLocation, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f)
            wp.sendMessage(s"&bランクが &a${data._1 + 1} &bに上がりました！")
            (data._1 + 1, 0)
          } else {
            // サイドバーに表記
            // 現在は8 - 1...
            sb.resetScores(chatColor(s"&9EXP: &a${data._2} &7/ &a${getNextExp(data._1)}"))
            obj.getScore(chatColor(s"&9EXP: &b&l$amount + ${data._2} &7/ &a${getNextExp(data._1)}")).setScore(7)
            player.playSound(player.getLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
            wp.sendMessage(s"&b$amount exp獲得しました")
            (data._1, nexp)
          }
          new BukkitRunnable {
            override def run(): Unit = {
              plugin.database.setRankData(uuid, ndata)
              updateSidebar(sb, ndata)
            }
          }.runTaskLater(plugin, 40L)
        case None =>
          wp.sendMessage("&cエラーメッセージです！")
      }
    }
  }.runTaskLater(plugin, 1L)


  /**
   * データを直接渡すため高速
   *
   * @param sb
   * @param data
   */
  def updateSidebar(sb: Scoreboard, data: (Int, Int)): Unit = {
    val test = new Test()
    if (sb.getObjective(DisplaySlot.SIDEBAR) != null) sb.getObjective(DisplaySlot.SIDEBAR).unregister()
    val obj = sb.registerNewObjective("sidebar", "dummy")
    obj.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aWelcome to &dWars &eβ"))
    obj.setDisplaySlot(DisplaySlot.SIDEBAR)

    val scores = List(
      obj.getScore(chatColor(s"&9Rank: &b${data._1}")),
      obj.getScore(chatColor(s"&9EXP: &a${data._2} &7/ &a${getNextExp(data._1)}")),
      obj.getScore(" "),
      obj.getScore(chatColor("&6/game&f: 試合に参加")),
      obj.getScore(chatColor("&6/spawn&f: スポーン地点に戻る")),
      obj.getScore(chatColor("&6/sf&f: ステータスを設定")),
      obj.getScore(chatColor("&6/pp&f: コマンド一覧を表示")),
      obj.getScore(chatColor("&6&m/vote&f: 投票ページを開く"))
    )
    var sc = scores.length
    scores.foreach(s => {
      s.setScore(sc)
      sc -= 1
    })
    test.log("RankManager.updateSidebar(Player, Scoreboard, (Int, Int))")
  }
}
