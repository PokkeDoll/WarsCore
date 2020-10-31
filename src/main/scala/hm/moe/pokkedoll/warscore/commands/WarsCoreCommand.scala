package hm.moe.pokkedoll.warscore.commands

import java.util.UUID

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.{WarsCore, WarsCoreAPI}
import hm.moe.pokkedoll.warscore.utils.{EconomyUtil, MerchantUtil, RankManager}
import io.chazza.advancementapi.Trigger.{TriggerBuilder, TriggerType}
import io.chazza.advancementapi.{AdvancementAPI, FrameType, Trigger}
import org.bukkit.{Bukkit, ChatColor, NamespacedKey}
import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.{EntityType, Player}
import org.bukkit.metadata.FixedMetadataValue

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
          if (args(0) == "config" || args(0) == "conf") {
            if(args.length > 1 && (args(1) == "reload")) {
              WarsCore.instance.reloadConfig()
              player.sendMessage(ChatColor.BLUE + "コンフィグをリロードしました。")
              WarsCoreAPI.reloadMapInfo(WarsCore.instance.getConfig.getConfigurationSection("mapinfo"))
              player.sendMessage(ChatColor.BLUE + "マップ情報をリロードしました。")
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
          } else if (args(0) == "vp") {
            val out = ByteStreams.newDataOutput
            out.writeUTF("TakeVotePoint")
            player.sendPluginMessage(WarsCore.instance, WarsCore.LEGACY_TORUS_CHANNEL, out.toByteArray)
          } else if (args(0) == "rs") {
            val out = ByteStreams.newDataOutput
            out.writeUTF("ResourcePack")
            player.sendPluginMessage(WarsCore.instance, WarsCore.LEGACY_TORUS_CHANNEL, out.toByteArray)
          } else if (args(0) == "cmdesp") {
            if(player.hasMetadata("cmdesp")) {
              player.removeMetadata("cmdesp", WarsCore.instance)
              player.sendMessage(ChatColor.BLUE + "CMDESPを無効化しました")
            } else {
              player.setMetadata("cmdesp", new FixedMetadataValue(WarsCore.instance, "true"))
              player.sendMessage(ChatColor.BLUE + "CMDESPを有効化しました")
            }
          } else if (args(0) == "changedisplayname" || args(0) == "cd") {
            if(args.length > 1) {
              val entities = player.getNearbyEntities(1d, 1d, 1d).stream().filter(p => p.getType != EntityType.PLAYER).findFirst()
              entities.ifPresent(t => {
                player.sendMessage(s"${t.getLocation.toString}のエンティティの名前を変更しました。")
                t.setCustomNameVisible(true)
                t.setCustomName(ChatColor.translateAlternateColorCodes('&', args.tail.mkString(" ")))
              })
            }
          }
        }
    }
    true
  }
}
