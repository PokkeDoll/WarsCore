package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import org.bukkit.Bukkit
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

/**
 * 他プレイヤーを招待するためのコマンド
 */
class InviteCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if(args.length == 0) {
          player.sendMessage("§c使用方法: /invite §a<プレイヤー名>")
        } else {
          Option(Bukkit.getPlayer(args(0))) match {
            case Some(target) =>
              val wp = WarsCoreAPI.getWPlayer(player)
              val tp = WarsCoreAPI.getWPlayer(target)
              if(wp.game.isEmpty) {
                player.sendMessage("§cゲームに参加していないため招待できませんでした")
              } else if (tp.game.isDefined) {
                player.sendMessage(s"§a${target.getName}§cはすでにゲーム(id: ${tp.game.get.id}に参加しています！")
              } else {
                // TODO
              }
            case None =>
              player.sendMessage("")
          }
        }
      case _ =>
    }
   true
  }
}
