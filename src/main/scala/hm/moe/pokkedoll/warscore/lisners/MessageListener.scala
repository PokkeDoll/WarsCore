package hm.moe.pokkedoll.warscore.lisners

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.WarsCore
import hm.moe.pokkedoll.warscore.utils.ItemUtil
import org.bukkit.{Bukkit, ChatColor, Sound}
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

/**
 * BungeeCordにお問い合わせしてクライアントのバージョンを得る
 * @param plugin
 */
class MessageListener(val plugin: WarsCore) extends PluginMessageListener {

  override def onPluginMessageReceived(channel: String, player: Player, message: Array[Byte]): Unit = {
    //plugin.getLogger.info(s"event received!! + $channel + ${player.getName}")
    if(!channel.equalsIgnoreCase(WarsCore.LEGACY_TORUS_CHANNEL)) return
    val in = ByteStreams.newDataInput(message)
    val subChannel = in.readUTF()

    if (subChannel == "TakeVotePoint") {
      if(in.readBoolean()) {
        player.sendMessage(ChatColor.BLUE + "VP一つ消費しました\nポールクリスタルを一つ獲得しました")
        ItemUtil.getItem("vote").foreach(player.getInventory.addItem(_))
        player.playSound(player.getLocation, Sound.BLOCK_NOTE_HARP, 1f, 2f)
      } else {
        player.sendMessage(ChatColor.RED + "VP が足りません！")
        player.playSound(player.getLocation, Sound.ENTITY_VILLAGER_NO, 1f ,1f)
      }
    }
  }

  val versionMap = Map(
    107->("1.9", 2),
    110->("1.9.3 ~ 1.9.4", 2),
    210->("1.10 ~ 1.10.2", 2),
    315->("1.11", 3),
    316->("1.11.1 ~ 1.11.2", 3),
    335->("1.12", 3),
    338->("1.12.1", 3),
    340->("1.12.2", 3),
    393->("1.13", 4),
    401->("1.13.1", 4),
    404->("1.13.2", 4),
    477->("1.14", 4),
    480->("1.14.1", 4),
    485->("1.14.2", 4),
    490->("1.14.3", 4),
    498->("1.14.4", 4),
    578->("1.15.2", 5),
    731->("1.16", 5),
    736->("1.16.1", 5)
  )
}
