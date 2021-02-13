package hm.moe.pokkedoll.warscore.games

object GameRewardType {
  object KILL extends GameRewardType

  object WIN extends GameRewardType

  object LOSE extends GameRewardType
}

sealed abstract class GameRewardType

