package hm.moe.pokkedoll.warscore.commands

import java.util.Optional

import hm.moe.pokkedoll.warscore.utils.MerchantUtil
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.{ChatColor, Material}
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.{Player, Villager}

class MerchantCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        val send = sendMessage(player, _)
        if(args.length == 0) {
          send(
            "&f/merchant <list|add|del|mod>\n"
          )
        } else if (args(0) == "list") {
          val comp = new ComponentBuilder("Show merchants list\n")
          MerchantUtil.config.getKeys(false).forEach(
            k => comp.append("* ").append(k).append("\n")
          )
        } else if (args(0) == "add") {
          if(args.length > 1) {
            if(!MerchantUtil.config.contains(args(1))) {
              MerchantUtil.config.set(args(1), java.util.Arrays.asList("air,air,air"))
              MerchantUtil.saveConfig()
              player.sendMessage(ChatColor.BLUE + args(1) + "を追加")
            } else {
              player.sendMessage(ChatColor.RED + args(1) + "はすでに存在している。delで削除")
            }
          } else {
            player.sendMessage(ChatColor.RED + "追加する取引タイトルを入力！")
          }
        } else if (args(0) == "del") {
          if(args.length > 1) {
            if(MerchantUtil.config.contains(args(1))) {
              MerchantUtil.config.set(args(1), null)
              MerchantUtil.saveConfig()
              player.sendMessage(ChatColor.BLUE + args(1) + "を削除")
            } else {
              player.sendMessage(ChatColor.RED + args(1) + "は存在しない")
            }
          } else {
            player.sendMessage(ChatColor.RED + "削除する取引タイトルを入力！")
          }
        } else if (args(0) == "mod") {
          if(args.length > 1) {
            if(MerchantUtil.config.contains(args(1))) {
              val key = args(1)
              if(args.length > 2) {
                if(args.length > 4 && args(2) == "set") {
                  val a3 = args(3)  // オペランド
                  val a4 = args(4)  // 値
                  if(a3 == "+" || a3 == "add") {
                    val content = MerchantUtil.config.getStringList(key)
                    content.add(a4)
                    MerchantUtil.config.set(key, content)
                    MerchantUtil.saveConfig()
                  } else if (a3 == "-" || a3 == "rem" || a3 == "rm") {
                    val content = MerchantUtil.config.getStringList(key)
                    content.remove(a4)
                    MerchantUtil.config.set(key, content)
                    MerchantUtil.saveConfig()
                  }
                } else if(args.length > 3 && (args(2) == "rem" || args(2) == "rm")) {
                  player.sendMessage(ChatColor.RED + "err: 313")
                } else if(args(2) == "info" || args(2) == "list") {
                  player.sendMessage(ChatColor.RED + "err: 312")
                } else {
                  player.sendMessage(ChatColor.RED + "err: 311")
                }
              } else {
                player.sendMessage(ChatColor.RED + "err: 310")
              }
            } else {
              player.sendMessage(ChatColor.RED + args(1) + "は存在しない")
            }
          } else {
            player.sendMessage(ChatColor.RED + "取引タイトルを入力！")
          }
        }
      case _ =>
    }
    true
  }

  private def sendMessage(player: Player, msg: String): Unit = {
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg))
  }

  private def getVillager(player: Player): Optional[Villager] = {
    player.getLocation.getNearbyEntitiesByType(classOf[Villager], 1d).stream().findFirst()
  }
}
