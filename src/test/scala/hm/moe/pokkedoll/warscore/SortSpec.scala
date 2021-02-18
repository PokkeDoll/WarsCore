package hm.moe.pokkedoll.warscore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.diagrams.Diagrams

class SortSpec extends AnyFlatSpec with Diagrams {

  val seq = Seq(("It is 0", 0), ("It is 5", 5), ("It is 10", 10))

  "リストのソートをチェック" should "trueを返す" in {
    assert({
      println(seq.sortBy(_._2))
      true
    })
  }

  "速度をチェックする" should "trueを返す" in {
    val mutableMap = scala.collection.mutable.HashMap.empty[String, String]
    var immutableMap = scala.collection.immutable.HashMap.empty[String, String]
    val mutableMapStart = System.currentTimeMillis()
    0 to 10000000 foreach(f => {
      mutableMap.put(s"Key is ${f}", s"Value is ${f}")
    })
    val mutableMapEnd = System.currentTimeMillis()

    val immutableMapStart = System.currentTimeMillis()
    0 to 10000000 foreach(f => {
      immutableMap += s"Key is ${f}" -> s"Value is ${f}"
    })
    val immutableMapEnd = System.currentTimeMillis()
    println(
      "結果発表ぉぉぉぉぉぉぉぉぉぉぉぉぉ！\n" +
        s"mutable.Map: ${mutableMapEnd - mutableMapStart}ms\n" +
        s"immutable.Map: ${immutableMapEnd - immutableMapStart}ms\n"
    )
    true
  }
}
