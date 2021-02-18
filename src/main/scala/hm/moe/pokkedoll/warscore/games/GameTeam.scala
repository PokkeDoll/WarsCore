package hm.moe.pokkedoll.warscore.games

object GameTeam {
  case object DEFAULT extends GameTeam("default")
  case object RED extends GameTeam("red")
  case object BLUE extends GameTeam("blue")

  def of(name: String): GameTeam = new GameTeam(name) {}

  def valueOf(name: String): GameTeam = {
    name match {
      case "red" => RED
      case "blue" => BLUE
      case _ => DEFAULT
    }
  }
}

sealed abstract class GameTeam(val name: String)

