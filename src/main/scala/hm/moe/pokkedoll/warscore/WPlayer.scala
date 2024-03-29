package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games.Game
import hm.moe.pokkedoll.warscore.wplayer.WPlayerState
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.ChatColor
import org.bukkit.entity.Player


/**
 * パッケージ名がhm.moe.pokkedoll.warscore.wplayer.WPlayerになるよ
 * WarsCoreのキャッシュを保存するクラス
 */
class WPlayer(val player: Player) {

  var game: Option[Game] = None

  /**
   * ゲームインスタンスを取得する
   * @return ゲームに参加していないならnull。必ずチェックすること！
   */
  def getGameAsJava: Game = game.orNull

  var changeInventory = true

  /**
   * 現在のランクのキャッシュ
   */
  var rank = 0

  /**
   * 現在の経験値のキャッシュ
   */
  var exp = 0

  var tag = ""

  var isShowTag = false

  /**
   * sendMessage(component: net.kyori.adventure.text.Component)を使え
   * @param string
   */
  @Deprecated
  def sendMessage(string: String): Unit = {
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', string))
  }

  /**
   * sendMessage(component: net.kyori.adventure.text.Component)を使え
   * @param components
   */
  @Deprecated
  def sendMessage(components: BaseComponent*): Unit = {
    player.sendMessage(components: _*)
  }

  def sendMessage(component: Component): Unit = {
    player.sendMessage(component)
  }

  var disconnect: Boolean = false

  var state: WPlayerState = WPlayerState.ONLINE
}
