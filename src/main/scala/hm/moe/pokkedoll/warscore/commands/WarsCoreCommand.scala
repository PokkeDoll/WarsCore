package hm.moe.pokkedoll.warscore.commands

import java.util.UUID

import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.utils.{EconomyUtil, MerchantUtil, RankManager}
import io.chazza.advancementapi.Trigger.{TriggerBuilder, TriggerType}
import io.chazza.advancementapi.{AdvancementAPI, FrameType, Trigger}
import org.bukkit.{Bukkit, ChatColor, NamespacedKey}
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

class WarsCoreCommand extends CommandExecutor {
  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    sender match {
      case player: Player =>
        if(args.length == 0) {
          player.sendMessage(
            ChatColor.translateAlternateColorCodes('&',
              "HELP\n" +
                "/whelp [economy|eco] [get] [ingot|coin] [amount]\n" +
                "* 自身にぽっけコインを与えます"
            )
          )
        } else {
          if (args(0) == "economy" || args(0) == "eco") {
            println("eco")
            if(args.length > 3 && args(1) == "get") {
              println("get")
              val eco = if(args(2) == "ingot") EconomyUtil.INGOT else EconomyUtil.COIN
              println("val eco")
              try {
                val amount = args(3).toInt
                println("val amount")
                EconomyUtil.give(player, eco, amount)
                println("EconomyUtil.give")
              } catch {
                case e: NumberFormatException =>
                  e.printStackTrace()
              }
            }
          } else if (args(0) == "config" || args(0) == "conf") {
            if(args.length > 1 && (args(1) == "reload")) {
              WarsCore.instance.reloadConfig()
              player.sendMessage(ChatColor.BLUE + "リロードしました。")
              MerchantUtil.merchantCache.clear()
            }
          } else if (args(0) == "exp") {
            if(args.length > 1) {
              RankManager.giveExp(WarsCoreAPI.getWPlayer(player), args(1).toInt)
            }
          } else if (args(0) == "a" ) {
            val advancement = AdvancementAPI.builder(new NamespacedKey(WarsCore.instance, "story/" + UUID.randomUUID().toString))
              .frame(FrameType.TASK)
              //.trigger(Trigger.builder(TriggerType.IMPOSSIBLE, "default"))
              .icon("minecraft:bow")
              .title("てすとTDM-なんとかで試合が始まりました！ /game tdm-あ とかで参加？ましょう")
              .description("description")
              .background("minecraft:textures/blocks/bedrock.png")
              .announce(false)
              .toast(true)
              .build()
            import collection.JavaConverters._
            advancement.show(WarsCore.instance, Bukkit.getOnlinePlayers.asScala.toSeq:_*)
            //advancement.grant(Bukkit.getOnlinePlayers.asScala.toSeq:_*)
          }
        }
    }
    true
  }
}
