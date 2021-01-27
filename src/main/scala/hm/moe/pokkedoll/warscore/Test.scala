package hm.moe.pokkedoll.warscore

import java.util.logging.Logger

/**
 * 実行時間を計測するクラス
 */
class Test(val message: String) {

  def this() {
    this("Unknown")
  }

  private val start = System.currentTimeMillis()

  def log(ms: Long): Unit = {
    val time = System.currentTimeMillis() - start
    if (time > ms) {
      Test.logger.warning(message + s"took $time ms grater than $ms ms!")
    }
  }

  def log(): Unit = {
    Test.logger.info(message + s"took ${System.currentTimeMillis() - start} ms!")
  }
}

object Test {
  protected lazy val logger: Logger = WarsCore.instance.getLogger
}
