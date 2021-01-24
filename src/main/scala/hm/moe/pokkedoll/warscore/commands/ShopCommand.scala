package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.WarsCoreAPI
import hm.moe.pokkedoll.warscore.ui.ShopUI
import hm.moe.pokkedoll.warscore.utils.ShopUtil
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class ShopCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if(args.length == 0) {

        } else if(args(0) == "list") {
          val sb = new StringBuilder("Shop list\n")
          ShopUtil.config.getKeys(false).forEach(f => {
            sb.append(s"* ${f}\n")
          })
          player.sendMessage(sb.toString())
        } else if(args(0) == "add") {
          if(args.length > 1) {
            val name = args(1)
            WarsCoreAPI.debug(player, "重複未チェック！")
            ShopUtil.newShop(name)
            WarsCoreAPI.info(player, s"$name を追加")
          }
        } else if(args(0) == "del") {
          if(args.length > 1) {
            val name = args(1)
            if(ShopUtil.config.contains(name)) {
              ShopUtil.delShop(name)
              WarsCoreAPI.info(player, s"$name を削除")
            } else {

            }
          } else {

          }
        } else if (args(0) == "mod") {
          if(args.length > 1) {
            val name = args(1)
            if(ShopUtil.config.contains(name)) {
              if(args.length > 2) {
                val string = args(2)
                val content = ShopUtil.config.getStringList(name)
                if(string.startsWith("+")) {
                  val text = string.substring(1)
                  "(primary|secondary|melee|grenade|item|head):.*@[0-9]*,(.*@[0-9]*,)*.*@[0-9]:\\d".r.findFirstMatchIn(text) match {
                    case Some(_) =>
                      content.add(text)
                      ShopUtil.setShop(name, content)
                      WarsCoreAPI.info(player, "追加しました")
                    case None =>
                      WarsCoreAPI.error(player, "構文が間違っています！")
                  }

                } else if (string.startsWith("-")) {
                  val index = WarsCoreAPI.parseInt(string.substring(1))
                  if(index != -1) {
                    content.remove(index)
                    ShopUtil.setShop(name, content)
                    WarsCoreAPI.info(player, "削除しました")
                  }
                } else if (string.startsWith("i")) {
                  val sb = new StringBuilder(s"Shop: $name\n")
                  (0 until content.size()).foreach(f => {
                    sb.append(s"$f. ${content.get(f)}\n")
                  })
                  player.sendMessage(sb.toString())
                }
              }
            }
          }
        } else if (args(0) == "open") {
          if(args.length > 1) {
            if(ShopUtil.hasName(args(1))) {
              ShopUI.openShopUI(player, args(1))
            }
          }
        }
    }
    true
  }
}
