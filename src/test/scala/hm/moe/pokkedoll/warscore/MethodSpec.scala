package hm.moe.pokkedoll.warscore

import org.scalatest.diagrams.Diagrams
import org.scalatest.flatspec.AnyFlatSpec

class MethodSpec extends AnyFlatSpec with Diagrams {


  class A {
    def returnTest(i: Int): Unit = {
      if(i == 0) {
        return
      } else {
        println(i)
      }
    }
  }

  class B extends A {
    override def returnTest(i: Int): Unit = {
      if(i == 1) {
        println(i)
      } else {
        println("n")
      }
    }
  }

  class C extends A {
    override def returnTest(i: Int): Unit = {
      super.returnTest(i)
      if(i == 1) {
        return
      } else {
        println(i)
      }
    }
  }

  "オーバーロードしないreturnを試す" should "" in {
    val b = new B
    b.returnTest(0)
    println("p1")
    b.returnTest(1)
    println("p2")
    true
  }

  "オーバーロードしたreturnを試す" should "" in {
    val c = new C
    c.returnTest(0)
    println("p1")
    c.returnTest(1)
    println("p2")
    true
  }

  "オーバーロードしたreturnを試す2" should "" in {
    val c = new C
    c.returnTest(2)
    println("p1")
    c.returnTest(1)
    println("p2")
    true
  }
}
