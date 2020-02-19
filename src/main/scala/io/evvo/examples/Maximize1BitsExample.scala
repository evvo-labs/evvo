package io.evvo.examples

import com.redis.RedisClient
import io.evvo.builtin.bitstrings.{Bitflipper, Bitstring, BitstringGenerator}
import io.evvo.builtin.deletors.DeleteDominated
import io.evvo.island.{
  AllowAllImmigrationStrategy,
  EvvoIsland,
  LogPopulationLoggingStrategy,
  RandomSampleEmigrationStrategy,
  StopAfter
}
import io.evvo.island.population.{Maximize, Objective}
import io.evvo.migration.redis.{RedisEmigrator, RedisImmigrator, RedisParetoFrontierRecorder}

import scala.concurrent.duration._

object Maximize1BitsExample {

  object Maximize1Bits extends Objective[Bitstring]("1Bits", Maximize()) {
    override protected def objective(sol: Bitstring): Double = {
      sol.bits.count(identity) // Bitstrings are represented as Seq[Boolean], count `true`s
    }
  }

  def main(args: Array[String]): Unit = {
    // Connect to redis
    val redisClient = new RedisClient(args(0), args(1).toInt)

    val duration = args(2).toInt.seconds

    // Create 1 island to maximize bits

    val evvoIsland = new EvvoIsland[Bitstring](
      creators = Vector(BitstringGenerator(length = 128, proportionOnes = 0)),
      mutators = Vector(Bitflipper()),
      deletors = Vector(DeleteDominated()),
      fitnesses = Vector(Maximize1Bits),
      immigrator = new RedisImmigrator[Bitstring](redisClient),
      immigrationStrategy = AllowAllImmigrationStrategy(),
      emigrator = new RedisEmigrator[Bitstring](redisClient),
      emigrationStrategy = RandomSampleEmigrationStrategy(32),
      loggingStrategy = LogPopulationLoggingStrategy(),
      paretoFrontierRecorder = new RedisParetoFrontierRecorder[Bitstring](redisClient)
    )

    // Run
    evvoIsland.runBlocking(StopAfter(duration))
    println(evvoIsland.currentParetoFrontier().toCsv())
    sys.exit(0)
  }
}
