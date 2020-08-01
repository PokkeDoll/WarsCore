package hm.moe.pokkedoll.warscore.lisners

import com.google.common.io.ByteStreams
import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

/**
 * BungeeCordにお問い合わせしてクライアントのバージョンを得る
 * @param plugin
 */
class MessageListener(val plugin: WarsCore) extends PluginMessageListener {
  override def onPluginMessageReceived(channel: String, player: Player, message: Array[Byte]): Unit = {
    plugin.getLogger.info(s"event received!! + $channel")
    if(!channel.equalsIgnoreCase("pokkedoll:torus")) return;
    val in = ByteStreams.newDataInput(message)
    val subChannel = in.readUTF()
    if(subChannel == "PlayerVersion") {
      val version = in.readInt()
      val player = Bukkit.getPlayer(in.readUTF())
      if(player!=null) {
        plugin.getLogger.info(s"player is ${player.getName}, version is $version")
        versionMap.get(version) match {
          case Some(value) =>
            player.sendMessage(
              s"§9バージョンを取得しました！\n" +
              s"§a§l${value._1} §9に対応したリソースパックを送信しています"
            )
          case None =>
            player.sendMessage(
              s"§cバージョンの取得に失敗しました... 管理者に報告してください(err: $version is not mapping)\n" +
              "§a§l1.12.2 §9に対応したリソースパックを送信しています..."
            )
        }
      }
    }
  }

  val versionMap = Map(
    340->("1.12.2", 3),
    404->("1.13.2", 4),
    498->("1.14.4", 4),
    578->("1.15.2", 5),
    731->("1.16.1", 5)
  )
}
