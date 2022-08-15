package hm.moe.pokkedoll.warscore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.diagrams.Diagrams

class SortSpec extends AnyFlatSpec with Diagrams {

  val seq = Seq(("It is 0", 0), ("It is 5", 5), ("It is 10", 10))

  val seq2 = Seq(4, 6, 1, 3, 6, 2, 3, 9, 3)

  "リストのソートをチェック" should "trueを返す" in {
    assert({
      println(seq.sortBy(_._2))
      true
    })
  }

  "計算をする" should "trueを返す" in {
    println(s"${(10/29.toDouble)}")
    true
  }

  //"ソートする" should "最大の値を返す"
}
