package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore, WarsCoreAPI}
import net.md_5.bungee.api.ChatColor
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable

/**
 * ランクを大雑把に管理するオブジェクト
 *
 * @author Emorard
 */
object RankManager {

  private lazy val plugin = WarsCore.instance

  val rankMap = Map(
    0 -> "&fぽっけ市民",
    1 -> "&f二等兵",
    2 -> "&f一等兵",
    3 -> "&f上等兵",
    4 -> "&f特技兵",
    5 -> "&f兵長",
    6 -> "&f伍長",
    7 -> "&f軍曹",
    8 -> "&f曹長",
    9 -> "&f准尉",
    10 -> "&f少尉",
    11 -> "&f中尉",
    12 -> "&f大尉",
    13 -> "&f少佐",
    14 -> "&f中佐",
    15 -> "&f大佐",
    16 -> "&f准将",
    17 -> "&f少将",
    18 -> "&f中将",
    19 -> "&f大将",
    20 -> "&f元帥",
    21 -> "&f大元帥",
    22 -> "?")

  val getClassName: Int => String = (rank: Int) => rankMap.getOrElse(rank / 3, "-")

  /**
   * 次に必要な経験値を返す
   *
   * @param rank ランク
   * @return
   */
  @Deprecated
  def getNextExp(rank: Int): Int = (100 * Math.pow(1.05, rank)).toInt

  val nextExp: Int => Int = (rank: Int) => (if(67 > rank) 100 * Math.pow(1.05, rank) else 50000).toInt

  def giveExp(wp: WPlayer, exp: Int): Unit = {
    wp.exp += exp
    if(getNextExp(wp.rank) > wp.exp) {
      wp.player.playSound(wp.player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f)
      wp.sendMessage(ChatColor.BLUE + s"$exp exp獲得しました！")
    } else {
      wp.rank += 1
      wp.exp = 0
      wp.player.playSound(wp.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0f)
      wp.sendMessage(ChatColor.AQUA + s"ランクが${wp.rank}に上がりました！")
    }
    new BukkitRunnable {
      override def run(): Unit = {
        WarsCoreAPI.scoreboards.get(wp.player) match {
          case Some(scoreboard) =>
            WarsCoreAPI.updateSidebar(wp.player, scoreboard)
          case _ =>
        }
        plugin.database.setRankData(wp.player.getUniqueId.toString, (wp.rank, wp.exp))
      }
    }.runTaskLaterAsynchronously(plugin, 1L)
  }
}
