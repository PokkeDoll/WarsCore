package hm.moe.pokkedoll.warscore.utils

import scala.collection.mutable

/**
 * Q: なぜLoaction型じゃないんですか！？
 * A: Game.load()するたびにワールドの情報が変わるため、読み込みエラーが発生してしまう
 *
 * @param gameId ゲームの識別ID, TDMとかね
 * @param mapId マップのID, yamaとかtaniとか
 * @author Emorard
 */
class MapInfo(val gameId: String, val mapId: String) {
  var mapName: String = "Unknown"
  var authors: String = "Unknown"
  var locations = mutable.HashMap.empty[String, (Double, Double, Double, Float, Float)]
}


