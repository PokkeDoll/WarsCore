package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.WPlayer
import org.bukkit.entity.Player

/**
 * Java用に優しくラップする
 *
 * @param game ゲーム
 */
class Game4JWrapper(private val game: Game) {
  def getGame: Game = game

  /**
   * 参加しているメンバーを取得する
   *
   * @return 配列で返す
   */
  def getMembers: Array[WPlayer] = game.members.toArray

  /**
   * データを返す
   *
   * @param player 対象のプレイヤー
   * @tparam T GamePlayerDataを上界に持つ何か
   * @return GamePlayerDataまたはnull
   */
  def getData(player: Player): GamePlayerData = {
    game match {
      case tdm: TeamDeathMatch => tdm.data.get(player).orNull
      case tdm4: TeamDeathMatch4 => tdm4.data.get(player).orNull
      case dom: Domination => dom.data.get(player).orNull
    }
  }
}
