package hm.moe.pokkedoll.warscore.utils

import org.bukkit.{Location, World}

class WeakLocation(val x: Double = 0d, val y: Double = 0d, val z: Double = 0d, val yaw: Float = 0f, val pitch: Float = 0f) {
  def getLocation(world: World): Location = {
    new Location(world, x, y, z, yaw, pitch)
  }
}

object WeakLocation {
  def of(array: Array[String]): Option[WeakLocation] = {
    try {
      Some(new WeakLocation(array(0).toDouble, array(1).toDouble, array(2).toDouble, array(3).toFloat, array(4).toFloat))
    } catch {
      case e: IndexOutOfBoundsException =>
        e.printStackTrace()
        None
      case e: NumberFormatException =>
        e.printStackTrace()
        None
    }
  }

  def empty: WeakLocation = new WeakLocation()
}
