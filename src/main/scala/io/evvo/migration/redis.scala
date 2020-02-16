package io.evvo.migration

import java.util.UUID

import com.redis.RedisClient
import com.redis.serialization.Parse
import io.evvo.island.population.{Maximize, Minimize, Scored}
import org.json4s._
import org.json4s.native.Serialization
object redis {
  val ns = "evvo"
  val emigrationTargetsKey = f"${ns}::emigrationtargets"

  /**
    * Uses Redis as a message-passing system for emigration. Only works properly if all other
    * islands are using a RedisImmigrator.
    */
  class RedisEmigrator[Sol](redisClient: RedisClient) extends Emigrator[Sol] {
    override def emigrate(solutions: Seq[Scored[Sol]]): Unit = {
      if (solutions.nonEmpty) {
        implicit val formats: Formats =
          Serialization.formats(FullTypeHints(List(classOf[Minimize], classOf[Maximize])))

        val maybeTarget = redisClient.srandmember[String](emigrationTargetsKey)
        maybeTarget.foreach(target => {
          val data = solutions.map(Serialization.write[Scored[Sol]])
          if (data.length == 1) {
            redisClient.rpush(target, data.head)
          } else {
            redisClient.rpush(target, data.head, data.tail: _*)
          }
        })
      }
    }
  }

  /**
    * Uses Redis as a message-passing system for immigration. Only works properly if all other
    * islands are using a RedisEmigrator.
    */
  class RedisImmigrator[Sol: Manifest](redisClient: RedisClient)(implicit parser: Parse[Sol])
      extends Immigrator[Sol] {
    val uuid: UUID = UUID.randomUUID()

    /** What is the list value that this immigration UUID should pop from? */
    val queueKey: String = f"${ns}::immigration_queues::${uuid.toString}"
    redisClient.sadd(emigrationTargetsKey, queueKey)

    override def immigrate(numberOfImmigrants: Int): Seq[Scored[Sol]] = {
      implicit val formats: Formats =
        Serialization.formats(FullTypeHints(List(classOf[Minimize], classOf[Maximize])))
      val result = redisClient.pipeline(redisClient => {
        redisClient.lrange[String](this.queueKey, 0, numberOfImmigrants)
        redisClient.ltrim(this.queueKey, 0, numberOfImmigrants)
      })
      result.fold(Seq[Scored[Sol]]())(
        _.asInstanceOf[List[Option[List[Option[String]]]]].head
          .fold(Seq[Scored[Sol]]())(lrangeResult => {
            lrangeResult.collect {
              case Some(str) =>
                Serialization.read[Scored[Sol]](str)
            }
          }))
    }
  }
}
