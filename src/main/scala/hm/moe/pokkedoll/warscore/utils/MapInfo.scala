package hm.moe.pokkedoll.warscore.utils

/**
 * Q: なぜLoaction型じゃないんですか！？<br>
 * A: Game.load()するたびにワールドの情報が変わるため、読み込みエラーが発生してしまう。 <br>
 *
 * @note 単体では使わない。WarsCoreAPIで使う。
 * @see WarsCoreAPI#reloadMapinfo
 * @constructor ゲームの識別IDとマップIDを作成
 * @param mapId マップのID, yamaとかtaniとか
 * @author Emorard
 * @version 2
 */
//TODO MapInfo.ofの追加
class MapInfo(val mapId: String = "Unknown",
              val mapName: String = "Unknown",
              val authors: String = "Unknown",
              val locations: Map[String, WeakLocation] = Map.empty[String, WeakLocation],
              val metadata: Map[String, Any] = Map.empty[String, Any])

object MapInfo {
  def of(): MapInfo = {
    null
  }
}