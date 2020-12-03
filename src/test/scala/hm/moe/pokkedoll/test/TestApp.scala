package hm.moe.pokkedoll.test

object TestApp extends App {
  val s = Seq((1, 0), (2, 0), (3, 0), (4, 1), (5, 0), (6, 1)).groupBy(f => f._2 == 0)

  s.keys.foreach(f => println(f.getClass))

  val ss = Range(0, 45)

  ss.indices.foreach(f => println(f + 9))

}
