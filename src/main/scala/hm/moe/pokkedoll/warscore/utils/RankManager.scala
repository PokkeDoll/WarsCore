package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.{WPlayer, WarsCore}
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

  private lazy val chatColor = ChatColor.translateAlternateColorCodes('&', _)

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
        plugin.database.setRankData(wp.player.getUniqueId.toString, (wp.rank, wp.exp))
      }
    }.runTaskLaterAsynchronously(plugin, 1L)
  }
}
