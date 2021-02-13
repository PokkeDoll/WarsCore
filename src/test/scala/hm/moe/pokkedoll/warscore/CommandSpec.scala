package hm.moe.pokkedoll.warscore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.diagrams.Diagrams

class CommandSpec extends AnyFlatSpec with Diagrams {
  /**
   * 仮想的な関数を実装する
   * @return
   */
  private def modContext(input: String): Boolean = {
    val text = input.substring(1)
    ".*:(.*@.*)+:\\d".r.findFirstMatchIn(text).isDefined
  }

  "ShopCommandのmod関数の構文チェック" should "正規表現通りだったらtrueを返す" in {
    assert(modContext("+item:aiueo@19293,a@384,csdf@38492:0"))
    assert(modContext("+secondary:0@0,23@0:23"))
    assert(modContext("+primary:i@3:0"))
    assert(!modContext("+unco:cc:2222"))
  }
}
