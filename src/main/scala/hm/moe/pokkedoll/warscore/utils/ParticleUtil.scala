package hm.moe.pokkedoll.warscore.utils

import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.{Color, Particle}
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

object ParticleUtil {
  def protection(player: Player): Unit = {
    new BukkitRunnable {
      val pp: Double = Math.PI / 10  // 2π / 20
      val pr: Double = 2*Math.PI/10
      var x = 0
      override def run(): Unit = {
        if (x >= 20) {
          cancel()
        } else {
          val y = 2.0-(x/10d)
          val l = player.getLocation.add(Math.cos(pp*x), y, Math.sin(pp*x))
          val l2 = player.getLocation.add(Math.cos(pp*x - Math.PI), y, Math.sin(pp*x - Math.PI))
          player.getWorld.spawnParticle(Particle.REDSTONE, l, 10, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(Color.AQUA, 1))
          player.getWorld.spawnParticle(Particle.REDSTONE, l2, 10, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(Color.AQUA, 1))
          (0 to 10).foreach(i => {
            val l3 = player.getLocation.add(Math.cos(pr*i), y, Math.sin(pr*i))
            player.getWorld.spawnParticle(Particle.REDSTONE, l3, 1, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(Color.BLUE, 1))
          })
          x += 1
        }
      }
    }.runTaskTimer(WarsCore.instance, 0, 1L)
  }
}
