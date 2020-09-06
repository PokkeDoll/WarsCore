package hm.moe.pokkedoll.warscore.commands

import java.util.Optional

import hm.moe.pokkedoll.warscore.utils.MerchantUtil
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
            "&f/merchant &aapply &c<merchant-key>&f: Configからmerchant-keyに指定された取引セットを半径1mの村人一人に設定する\n" +
            "&f/merchant &a(head | helmet)&f: 手に持っているアイテム半径1mの村人一人に取り付ける\n" +
            "&f/merchant &aname &f: 半径1mの村人一人の名前を変更する\n"
          )
        } else if (args(0) == "apply") {
          val key = if(args.length > 1) args(1) else return true
          val target = getVillager(player)
          if(target.isEmpty) {
            player.sendMessage("空")
          } else {
            target.ifPresent(villager => {
              MerchantUtil.getMerchantRecipes(key) match {
                case Some(merchantRecipes) =>
                  villager.setRecipes(merchantRecipes)
                  player.sendMessage("セット！！！！！！！！！！！！！！！！！")
                  villager.setInvulnerable(true)
                case None =>
                  player.sendMessage("空！！！！！！！！")
              }
            })
          }
        } else if (args.length > 1 && args(0) == "name") {
          val target = getVillager(player)
          if(target.isEmpty) {
            player.sendMessage("村人が見つかりません")
          } else {
            target.ifPresent(villager => {
              if(args(1) == "reset") {
                villager.setCustomName(null)
              } else {
                villager.setCustomName(ChatColor.translateAlternateColorCodes('&', args(1)))
              }
            })
          }
        } else if(args(0) == "head" || args(0) == "helmet") {
          getVillager(player).ifPresent(vil => {
            val i = player.getInventory.getItemInMainHand
            if(i.getType == Material.AIR) {
              vil.getEquipment.setHelmet(null)
            } else {
              vil.getEquipment.setHelmet(i)
            }
          })
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
