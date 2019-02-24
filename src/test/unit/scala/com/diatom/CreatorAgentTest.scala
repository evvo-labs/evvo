package com.diatom

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import com.diatom.agent.func.CreatorFunc
import com.diatom.agent.{CreatorAgent, TCreatorAgent}
import com.diatom.population.PopulationActorRef
import org.scalatest.{FlatSpec, Matchers, WordSpecLike}
import scala.concurrent.duration._
import org.scalatest.BeforeAndAfter


class CreatorAgentTest extends TestKit(ActorSystem("CreatorAgentTest"))
  with WordSpecLike with Matchers with BeforeAndAfter {
  // solution type
  type S = Int

  val probe = TestProbe()
  val pop = PopulationActorRef[S](probe.ref)

  val create = () => Set(1)
  val creatorFunc = CreatorFunc(create)
  var creatorAgent: CreatorAgent[S] = _

  before {
    creatorAgent = CreatorAgent(creatorFunc, pop)
  }

  val expectedMessage = PopulationActorRef.AddSolutions(create())

  "A Creator Agent" should {
    "send the output of its creation function to the population when told to step" in {
      // step should work multiple times
      for (_ <- 1 to 3) {
        creatorAgent.step()
        probe.expectMsg(expectedMessage)
      }
    }

    "step when told to start, stop stepping when told to stop" in {
      creatorAgent.start()
      probe.expectMsg(3.seconds, expectedMessage)
      creatorAgent.stop()
      probe.expectNoMessage(3.seconds)
    }

    "shouldn't do anything if started twice" in {
      creatorAgent.start()
      creatorAgent.start()

      creatorAgent.stop()
      probe.expectNoMessage(3.seconds)
    }

    "shouldn't break if stopped twice" in {
      creatorAgent.start()
      creatorAgent.stop()
      creatorAgent.stop()
    }
  }
}
