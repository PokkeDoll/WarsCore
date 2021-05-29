package hm.moe.pokkedoll.warscore.games

import org.bukkit.entity.Player

import scala.collection.mutable

trait GamePlayerData {
  // 順に, キル, デス, アシスト, ダメージ量, 受けたダメージ量
  var kill, death, assist: Int = 0

  var damage, damaged: Double = 0d

  var win = false

  var damagedPlayer = mutable.Set.empty[Player]

  var team: GameTeam = GameTeam.DEFAULT
}
