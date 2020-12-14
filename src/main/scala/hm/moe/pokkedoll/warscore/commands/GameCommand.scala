package hm.moe.pokkedoll.warscore.commands

import java.util

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import hm.moe.pokkedoll.warscore.ui.GameUI
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandExecutor, CommandSender, ConsoleCommandSender, TabCompleter}
import org.bukkit.entity.Player

/**
 * ゲームのメインとなるコマンド
 *
 * @author Emorard
 */
class GameCommand extends CommandExecutor with TabCompleter {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if (args.length == 0) {
          GameUI.openMainUI(player)
        } else if (args(0) == "list") {
          val sb = new StringBuilder("§bゲームリスト")
          WarsCoreAPI.games.values.foreach(game => {
            sb.append(s"§a${game.id}§7: §f${game.title}§7: §f${game.mapInfo.mapName}§7: §f${game.members.size} / ${game.maxMember}\n")
          })
          player.sendMessage(sb.toString())
        } else if (args(0).length >= 1 && (args(0) == "leave" || args(0) == "quit")) {
          val wp = WarsCoreAPI.getWPlayer(player)
          wp.game match {
            case Some(game) =>
              game.hub(wp)
            case None =>
              player.sendMessage("§cゲームに参加していません！")
          }
        } else if (args(0).length >= 2 && (args(0) == "delete" || args(0) == "d") && player.hasPermission("pokkedoll.game.admin")) {
          val wp = WarsCoreAPI.getWPlayer(player)
          wp.game match {
            case Some(game) =>
              game.end()
            case None =>
              player.sendMessage(ChatColor.RED + "ゲームに参加していません！")
          }
        }  else if (args.length >= 2 && args(0) == "join") {
          WarsCoreAPI.games.get(args(1)) match {
            case Some(game) =>
              game.join(player)
            case None =>
              player.sendMessage(s"${args(1)}は存在しません！")
          }
        } else if (args.length >= 2 && args(0) == "end" && player.hasPermission("pokkedoll.game.admin")) {
          WarsCoreAPI.games.get(args(1)) match {
            case Some(game) =>
              game.disable = true
              player.sendMessage(ChatColor.BLUE + s"${args(1)}を無効化しました")
            case None =>
              player.sendMessage(ChatColor.RED + s"${args(1)} は存在しません！")
          }
        } else if (args(0) == "who") {
          who(sender)
        }
      case console: ConsoleCommandSender =>
        if (args(0) == "who") {
          who(sender)
        }
    }
    true
  }

  def who(sender: CommandSender): Unit = {
    val comp = new ComponentBuilder("Who\n")
    WarsCoreAPI.wplayers.foreach(f => {
      comp.append(s"${f._1.getName}: ${
        f._2.game match {
          case Some(game) => game.id
          case None => "None"
        }
      }\n")
    })
    sender.sendMessage(comp.create(): _*)
  }

  import scala.jdk.CollectionConverters._

  override def onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array[String]): util.List[String] = {
    if (command.getName.equalsIgnoreCase("game")) {
      sender match {
        case _: Player =>
          if (args.length == 0) {
            return util.Arrays.asList("join", "leave", "info")
          }
          if (args(0).startsWith("join")) {
            return WarsCoreAPI.games.keys.toList.asJava
          }
      }
    }
    null
  }
}
