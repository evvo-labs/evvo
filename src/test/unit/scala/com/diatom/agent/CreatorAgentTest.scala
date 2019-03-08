package com.diatom.agent

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.diatom.agent.func.CreatorFunc
import com.diatom.population.PopulationActorRef
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers, WordSpecLike}

import scala.concurrent.duration._

// TODO FIXME, I think it may be impossible to test our actors without message passing
// stuff... which is not ideal. Possible we should move all the logic to components,
// and have the agents delegate as well? Though then we have wrappers on both sides of the message.
class CreatorAgentTest extends TestKit(ActorSystem("CreatorAgentTest"))
  with WordSpecLike with Matchers with BeforeAndAfter {
  // solution type
  type S = Int

  val probe = TestProbe()
  val pop = PopulationActorRef[S](probe.ref)

  val create = () => Set(1)
  val creatorFunc = CreatorFunc(create)
  var creatorAgent: TAgent[S] = _

  before {
    creatorAgent = AgentActorRef(CreatorAgent.from(creatorFunc, pop, _ => 50.millis))
  }

  val expectedMessage = PopulationActorRef.AddSolutions(create())


//  "A Creator Agent" should {
//    "send the output of its creation function to the population when told to step" in {
//      // step should work multiple times, 3 is arbitrary
//      for (_ <- 1 to 3) {
//        creatorAgent.step()
//        probe.expectMsg(expectedMessage)
//      }
//    }
//  }
}
