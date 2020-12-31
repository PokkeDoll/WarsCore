package hm.moe.pokkedoll.test

object TestApp extends App {
  /*
  val s = Seq((1, 0), (2, 0), (3, 0), (4, 1), (5, 0), (6, 1)).groupBy(f => f._2 == 0)

  s.keys.foreach(f => println(f.getClass))

  val ss = Range(0, 45)

  ss.indices.foreach(f => println(f + 9))
   */
/*
  var dummy2 = Vector.empty[Int]
  var i = 0
  while (i <= 100) {
    i += 1
    dummy2 :+= i
  }
  print(dummy2)
 */
  var a = Seq.empty[Int]
  a :+= 5
  a :+= 99
  println(a)
}
