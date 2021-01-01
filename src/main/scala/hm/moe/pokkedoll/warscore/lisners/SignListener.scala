package hm.moe.pokkedoll.warscore.lisners

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.ui.WeaponUI
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import org.bukkit.ChatColor
import org.bukkit.block.{EnderChest, Sign}
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.{EventHandler, Listener}

class SignListener(plugin: WarsCore) extends Listener {

  @EventHandler
  def onClick(e: PlayerInteractEvent): Unit = {
    if (e.getClickedBlock != null) {
      e.getClickedBlock.getState match {
        case sign: Sign if e.getAction == Action.RIGHT_CLICK_BLOCK =>

          /**
           * テストケース
           */
          try {
            val lines = sign.getLines
            // ここでIndexOut
            if (lines(0) == null) return
            if (lines(0).length <= 1) return
            // [<ゲームのID>]
            WarsCoreAPI.games.get(lines(0).substring(1, lines(0).length - 1)) match {
              case Some(game) =>
                if (game.join(e.getPlayer)) {
                }
              case None =>
            }
            if (lines(0).contains("[Vote Point]")) {
              val player = e.getPlayer
              if (player.getInventory.firstEmpty() == -1) {
                player.sendMessage(ChatColor.RED + "インベントリの空きが足りません！！")
              } else {
                val out = ByteStreams.newDataOutput
                out.writeUTF("TakeVotePoint")
                player.sendPluginMessage(WarsCore.instance, WarsCore.MODERN_TORUS_CHANNEL, out.toByteArray)
              }
            }
          } catch {
            case e: ArrayIndexOutOfBoundsException =>
              e.printStackTrace()
            case e: NullPointerException =>
              e.printStackTrace()
          }
          // TODO 何かしらは追加
          /*
        case _: EnderChest if e.getAction == Action.RIGHT_CLICK_BLOCK =>
          e.setCancelled(true)
          WeaponUI.openWeaponStorageUI(e.getPlayer)

           */
        case _ =>
      }
    }
  }
}
