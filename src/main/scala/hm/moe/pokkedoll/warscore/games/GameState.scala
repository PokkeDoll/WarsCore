package hm.moe.pokkedoll.warscore.games

/**
 * ゲームの状態を表す
 * @author Emorard
 */
object GameState {
  case object DISABLE extends GameState("disable", "§c無効化", false)
  case object LOADING_WORLD extends GameState("loading_world", "§eワールドの読み込み中", false)
  case object INIT extends GameState("init", "初期化中",false)
  case object STANDBY extends GameState("standby", "§a参加受付中！", true)
  @Deprecated
  case object WAIT extends GameState("wait", "§a受付中", true)
  case object READY extends GameState("ready", "§a準備中", true)
  @Deprecated
  case object PLAY extends GameState("play", "試合中!",true)
  @Deprecated
  case object PLAY2 extends GameState("play2", "試合中(参加不可)",false)
  case object PLAYING extends GameState("playing", "試合中",true)
  case object PLAYING_CANNOT_JOIN extends GameState("playing_cannot_join", "試合中(参加できません)",false)
  case object END extends GameState("end", "ゲーム終了", false)
  case object ERROR extends GameState("error", "エラーが発生しました", false)

  @Deprecated
  case object FREEZE extends GameState("freeze", "凍結中", false)

  def valueOf(name: String): GameState = name.toLowerCase match {
    case "init" => INIT
    case "wait" | "standby" => STANDBY
    case "ready" => READY
    case "play" => PLAY
    case "playing" => PLAYING
    case "play2" => PLAY2
    case "playing_cannot_join" => PLAYING_CANNOT_JOIN
    case "end" => END
    case "error" => ERROR
    case "freeze" => FREEZE
    case _ => DISABLE
  }
}

sealed abstract class GameState(val name: String, val title: String, val join: Boolean)

/*
v3.0用
enum GameState(val display: TextComponent, val canJoin: Boolean):
  case DISABLE extends GameState(Component.text("無効化").color(NamedTextColor.RED), false)
  case LOADING_WORLD extends GameState(Component.text("ワールドの読み込み中").color(NamedTextColor.YELLOW), false)
  case INIT extends GameState(Component.text("初期化中").color(NamedTextColor.YELLOW), false)
  case STANDBY extends GameState(Component.text("参加受付中！").color(NamedTextColor.GREEN), true)
  case READY extends GameState(Component.text("まもなく試合が始まります！").color(NamedTextColor.GREEN), true)
  case PLAYING extends GameState(Component.text("プレイ中").color(NamedTextColor.AQUA), true)
  case PLAYING_CANNOT_JOIN extends GameState(Component.text("プレイ中(参加できません)").color(NamedTextColor.GRAY), false)
  case END extends GameState(Component.text("ゲーム終了").color(NamedTextColor.YELLOW), false)
  case ERROR extends GameState(Component.text("エラー！").color(NamedTextColor.RED), false)
 */