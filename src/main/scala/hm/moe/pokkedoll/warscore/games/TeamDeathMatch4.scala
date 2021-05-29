package hm.moe.pokkedoll.warscore.games

import hm.moe.pokkedoll.warscore.utils._

/**
 * 4vs4で行うゲームモード <br>
 * version2.0では 10 vs 10というわけではない。<br>
 * version3.0ではコールバックシステムに対応。 <br>
 * 1点 = 1キル<br>
 * 10分 or 50点先取で勝利<br>
 *
 * @author Emorard
 * @version 3.0
 */
class TeamDeathMatch4(override val id: String) extends TeamDeathMatch(id) {
  /**
   * ゲームの構成
   */
  override val config: GameConfig = GameConfig.getConfig("tdm4")

  /**
   * ゲームのタイトル
   */
  override val title: String = "チームデスマッチ4"

  /**
   * 受け入れる最大人数
   */
  override val maxMember: Int = 8
}
