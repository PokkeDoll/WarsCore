package hm.moe.pokkedoll.test

object TestApp extends App {
  val map = Map(1 -> "1", 2 -> "2", 3 -> "3")


  val sl = map.filter(pred => pred._1 != 0).slice(0, 45).toIndexedSeq

  println(sl.size)
  println(sl(3))

}
