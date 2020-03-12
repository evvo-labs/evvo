package unit.scala.io.evvo.migration

import com.redis.RedisClient
import com.redis.serialization.Parse.Implicits._
import io.evvo.island.population.{Maximize, Scored}
import io.evvo.migration.redis
import io.evvo.migration.redis.{RedisEmigrator, RedisImmigrator}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.sys.process.Process
import scala.util.{Failure, Success}

object fixtures {
  val redisPort = 18734

  def withRedis(op: RedisClient => Unit): Unit = {
    val redisProcessBuilder = Process(Seq("redis-server", "--port", f"${redisPort}"))
    val redisProcess = redisProcessBuilder.run()
    val redisClient = new RedisClient("localhost", redisPort)

    try {
      Await.ready(
        Future(
          if (redisProcess.toString.contains("Ready to accept connections")) {
            Success(())

          } else {
            Failure
          }
        )(global),
        5.seconds
      )

      op(redisClient)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        assert(false)
    } finally {
      if (redisProcess.isAlive()) {
        redisProcess.destroy()
      }
    }
  }
}

class RedisMigrationTest extends WordSpec with Matchers {
  "Redis-based immigration and emigration" should {
    "retrieve solutions in the correct order" in {
      fixtures.withRedis(client => {
        client.keys[String]()
        val immigrator = new RedisImmigrator[String](client)
        immigrator.immigrate(2) shouldBe Seq()
        val emigrator = new RedisEmigrator[String](client)
        emigrator.emigrate(
          Seq(
            Scored[String](Map(("objective1", Maximize() -> 1)), "a"),
            Scored[String](Map(("objective1", Maximize() -> 2)), "b"),
            Scored[String](Map(("objective1", Maximize() -> 3)), "c")
          ))
        immigrator.immigrate(2).map(_.solution) shouldBe Seq("a", "b", "c")
      })
    }
  }
  // TODO add a test for multiple immigrators
}
