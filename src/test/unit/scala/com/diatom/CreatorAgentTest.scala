package com.diatom

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import com.diatom.agent.func.CreatorFunc
import com.diatom.agent.{CreatorAgent, TCreatorAgent}
import com.diatom.population.PopulationActorRef
import org.scalatest.{FlatSpec, Matchers, WordSpecLike}

class CreatorAgentTest extends TestKit(ActorSystem("CreatorAgentTest"))
  with WordSpecLike with Matchers {
  // solution type
  type S = Int

  val probe = TestProbe()
  val pop = PopulationActorRef[S](probe.ref)

  val creatorFunc = CreatorFunc(() => Set(1))
  val creatorAgent: CreatorAgent[S] = CreatorAgent(creatorFunc, pop)

  "A Creator Agent" should {
    "send the output of its creation function to the population when told to step" in {
      creatorAgent.step()
      probe.expectMsg(PopulationActorRef.AddSolutions(Set(1)))
    }
  }

}
