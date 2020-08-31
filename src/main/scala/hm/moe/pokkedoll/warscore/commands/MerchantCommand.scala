package hm.moe.pokkedoll.warscore.commands

import hm.moe.pokkedoll.warscore.utils.MerchantUtil
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.{Player, Villager}

class MerchantCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if(args.length == 0) {
          player.sendMessage("あ")
        } else if (args(0) == "apply") {
          val key = if(args.length > 1) args(1) else return true
          val target = player.getLocation().getNearbyEntitiesByType(classOf[Villager], 1d)
          if(target.isEmpty) {
            player.sendMessage("空")
          } else {
            target.stream().findFirst().ifPresent(villager => {
              MerchantUtil.getMerchantRecipes(key) match {
                case Some(merchantRecipes) =>
                  villager.setRecipes(merchantRecipes)
                  player.sendMessage("セット！！！！！！！！！！！！！！！！！")
                case None =>
                  player.sendMessage("空！！！！！！！！")
              }
            })
          }
        }
    }
    true
  }
}
