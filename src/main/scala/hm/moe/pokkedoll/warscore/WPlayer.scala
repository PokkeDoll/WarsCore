package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games.Game
import org.bukkit.entity.Player

/**
 * WarsCoreのキャッシュを保存するクラス
 */
class WPlayer(val player: Player) {

  var game: Option[Game] = None

  /**
   * TDMの情報ッッッッ！
   */
  var play, win, kill, death, assist, damage: Int = _

  def sendMessage(string: String): Unit = {
    player.sendMessage(string: String)
  }
}
