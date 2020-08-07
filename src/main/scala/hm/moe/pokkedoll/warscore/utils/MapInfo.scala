package hm.moe.pokkedoll.warscore.utils

import scala.collection.mutable

/**
 * Q: なぜLoaction型じゃないんですか！？<br>
 * A: Game.load()するたびにワールドの情報が変わるため、読み込みエラーが発生してしまう。 <br>
 *
 * @note 単体では使わない。WarsCoreAPIで使う。
 * @see WarsCoreAPI#reloadMapinfo
 *
 * @constructor ゲームの識別IDとマップIDを作成
 * @param gameId ゲームの識別ID, TDMなど
 * @param mapId マップのID, yamaとかtaniとか
 *
 * @author Emorard
 *
 *
 */
class MapInfo(val gameId: String, val mapId: String) {
  var mapName: String = "Unknown"
  var authors: String = "Unknown"
  var locations = mutable.HashMap.empty[String, (Double, Double, Double, Float, Float)]
}
