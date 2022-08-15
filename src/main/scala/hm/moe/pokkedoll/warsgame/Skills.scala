package hm.moe.pokkedoll.warsgame

import com.destroystokyo.paper.ClientOption
import hm.moe.pokkedoll.warscore.WarsCore
import org.bukkit.{Bukkit, Particle}
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

object Skills {
  def bh(player: Player): Unit = {
    val world = player.getWorld
    val eye = player.getEyeLocation.getDirection.multiply(-1)

    player.sendMessage(eye.toString)

    val eyepo = eye.clone().rotateAroundY(15d)
    val eyene = eye.clone().rotateAroundY(-15d)




    new BukkitRunnable {
      var i = 1
      private val el = player.getEyeLocation
      override def run(): Unit = {
        if(i <= 60) {
          //val a = eyepo.multiply(i).toLocation(world)
          //player.sendMessage(a.toString)
          world.spawnParticle(Particle.SPELL, el.add(eyepo.multiply(i)), 10, 0d, 0d, 0d, 0)
          world.spawnParticle(Particle.SPELL, el.add(eyene.multiply(i)), 10, 0d, 0d, 0d, 0)

          i+=1
        } else {
          cancel()
        }
      }
    }.runTaskTimer(WarsCore.getInstance, 0L, 2L)
  }
}
