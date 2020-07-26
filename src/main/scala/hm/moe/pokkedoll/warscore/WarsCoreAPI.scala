package hm.moe.pokkedoll.warscore

import hm.moe.pokkedoll.warscore.games.Game
import hm.moe.pokkedoll.warscore.utils.MapInfo
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.{Bukkit, Location}
import org.bukkit.scoreboard.{Scoreboard, Team}

import scala.collection.mutable

/**
 * 便利なメソッドをまとめるオブジェクト
 *
 * @version 1
 */
object WarsCoreAPI {
  // 内部バージョン
  val VERSION = 1

  lazy val scoreboard: Scoreboard = Bukkit.getScoreboardManager.getNewScoreboard

  def setBaseTeam(team: Team): Unit = {
    team.setAllowFriendlyFire(false)
    team.setCanSeeFriendlyInvisibles(true)
    team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
    team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS)
  }

  protected[warscore] var DEFAULT_SPAWN: Location = _

  val games = mutable.HashMap.empty[String, Game]

  val wplayers = mutable.HashMap.empty[Player, WPlayer]

  var worldSettingConfig: ConfigurationSection = _

  val mapinfo = mutable.Seq.empty[MapInfo]

  /**
   * テストコード
   * @param player
   * @return
   */
  def getWPlayer(player: Player): WPlayer = wplayers.getOrElseUpdate(player, new WPlayer(player))

  /**
   * 動きを止める
   * @param player
   */
  def freeze(player: Player): Unit = {
    player.teleport(player.getLocation().add(0, 0.001, 0))
    player.setAllowFlight(true)
    player.setFlying(true)
    player.setFlySpeed(0f)
  }

  /**
   * フリーズを解除する
   */
  def unfreeze(player: Player): Unit = {
    player.setAllowFlight(false)
    player.setFlying(false)
    player.setFlySpeed(0.1f)
  }
}
