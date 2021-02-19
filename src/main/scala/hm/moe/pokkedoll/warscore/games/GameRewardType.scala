package hm.moe.pokkedoll.warscore.games

object GameRewardType {
  case object KILL extends GameRewardType

  case object WIN extends GameRewardType

  case object LOSE extends GameRewardType

  case object ASSIST extends GameRewardType
}

sealed abstract class GameRewardType

