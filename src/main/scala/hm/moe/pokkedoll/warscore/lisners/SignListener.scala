package hm.moe.pokkedoll.warscore.lisners

import com.google.common.primitives.UnsignedInteger
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.ChatColor
import org.bukkit.block.Sign
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.scheduler.BukkitRunnable

class SignListener(plugin: WarsCore) extends Listener {

  @EventHandler
  def onClick(e: PlayerInteractEvent): Unit = {
    if(e.getClickedBlock == null) {
      e.getClickedBlock match {
        case sign: Sign =>
          /**
           * テストケース
           */
          try {
            val lines = sign.getLines
            // ここでIndexOut
            if(lines(0) == null) return
            // [<ゲームのID>]
            WarsCoreAPI.games.get(lines(1).substring(1, lines(1).length + 1)) match {
              case Some(game) =>
                if(!game.state.join) {
                  e.getPlayer.sendMessage("§cゲームに参加できません!")
                } else if(game.members.length >= game.maxMember) {
                  e.getPlayer.sendMessage("§c人数が満員なので参加できません！")
                } else {
                  game.join(e.getPlayer)
                  // 人数情報を更新する
                  new BukkitRunnable {
                    override def run(): Unit = {
                      lines(2) = s"§f${game.members.length} §0/ §f${game.maxMember}"
                      lines(3) = s"${game.state.title}"
                    }
                  }.runTaskLater(plugin, 20L)
                }
              case None =>
                e.getPlayer.sendMessage("§cゲーム情報が見つからないので参加できませんでした")
            }
          } catch {
            case e: ArrayIndexOutOfBoundsException =>
              e.printStackTrace()
            case e: NullPointerException =>
              e.printStackTrace()
          }
        case _ =>
      }
    }
  }
}
