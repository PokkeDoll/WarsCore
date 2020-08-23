package hm.moe.pokkedoll.warscore

import java.util.logging.Logger

/**
 * 実行時間を計測するクラス
 */
class Test {
  private val start = System.currentTimeMillis()

  def log(message: String = ""): Unit = {
    Test.logger.info(message + s"took ${System.currentTimeMillis() - start} ms!")
  }
}

object Test {
  protected lazy val logger = WarsCore.instance.getLogger
}
