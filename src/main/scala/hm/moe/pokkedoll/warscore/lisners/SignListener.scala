package hm.moe.pokkedoll.warscore.lisners

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.block.Sign
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.{EventHandler, Listener}

class SignListener(plugin: WarsCore) extends Listener {

  @EventHandler
  def onClick(e: PlayerInteractEvent): Unit = {
    if(e.getClickedBlock != null) {
      e.getClickedBlock.getState match {
        case sign: Sign =>
          /**
           * テストケース
           */
          try {
            val lines = sign.getLines
            // ここでIndexOut
            if(lines(0) == null) return
            if(lines(0).length < 1) return
            // [<ゲームのID>]
            WarsCoreAPI.games.get(lines(0).substring(1, lines(0).length - 1)) match {
              case Some(game) =>
                if(game.join(e.getPlayer)) {
                  // 人数情報を更新する
                  /*
                  new BukkitRunnable {
                    override def run(): Unit = {
                      lines(1) = s"§f${game.members.length} §0/ §f${game.maxMember}"
                      lines(2) = s"${game.state.title}"
                      sign.update()
                    }
                  }.runTaskLater(plugin, 20L)
                   */
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
