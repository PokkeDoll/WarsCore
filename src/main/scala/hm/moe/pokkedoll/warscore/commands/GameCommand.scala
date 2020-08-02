package hm.moe.pokkedoll.warscore.commands

import java.util

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import org.bukkit.command.{Command, CommandExecutor, CommandSender, TabCompleter}
import org.bukkit.entity.Player

/**
 * ゲームのメインとなるコマンド
 * @author Emorard
 */
class GameCommand extends CommandExecutor with TabCompleter {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if(args.length == 0) {
          WarsCoreAPI.showGameInventory(player)
        } else if(args(0) == "list") {
          val sb = new StringBuilder("§bゲームリスト")
          WarsCoreAPI.games.values.foreach(game => {
            sb.append(s"§a${game.id}§7: §f${game.title}§7: §f${game.mapInfo.mapName}§7: §f${game.members.size} / ${game.maxMember}\n")
          })
          player.sendMessage(sb.toString())
        } else if(args(0).length >= 1 && args(0) == "leave") {
          val wp = WarsCoreAPI.getWPlayer(player)
          wp.game match {
            case Some(game) =>
              game.hub(wp)
            case None =>
              player.sendMessage("§cゲームに参加していません！")
          }
        } else if (args.length >= 2 && args(0) == "join") {
          WarsCoreAPI.games.get(args(1)) match {
            case Some(game) =>
              game.join(player)
            case None =>
              player.sendMessage(s"${args(1)}は存在しません！")
          }
        }
      case _ =>
    }
    true
  }

  import collection.JavaConverters._

  override def onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array[String]): util.List[String] = {
    if(command.getName.equalsIgnoreCase("game")) {
      sender match {
        case player: Player =>
          if(args.length == 0) {
            return util.Arrays.asList("join", "leave", "info")
          }
          if(args(0).startsWith("join")) {
            return WarsCoreAPI.games.keys.toList.asJava
          }
      }
    }
    null
  }
}
