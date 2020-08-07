package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.{ClickEvent, ComponentBuilder}
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
                player.sendMessage(
                  new ComponentBuilder("= = = = = = = = = = = = = = = = = = = = =\n")
                    .append(s"${player.getName}").color(ChatColor.GREEN).append("から招待が届きました！").color(ChatColor.WHITE)
                    .append("ゲームモード: ").color(ChatColor.YELLOW).append(wp.game.get.title).color(ChatColor.GREEN).append("\n")
                    .append("参加するにはこのメッセージをクリック！！").bold(true).color(ChatColor.AQUA).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, s"/game join ${wp.game.get.id}"))
                    .create():_*
                )
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
