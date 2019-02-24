package com.diatom

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.diatom.agent.func.{CreatorFunc, DeletorFunc, MutatorFunc}
import com.diatom.agent._
import com.diatom.population.PopulationActorRef
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import scala.concurrent.duration._

class AgentPropertiesTest extends TestKit(ActorSystem("AgentPropertiesTest"))
  with WordSpecLike with Matchers with BeforeAndAfter {

  type S = Int

  var probe: TestProbe = _
  var pop: PopulationActorRef[S] = _

  val create: CreatorFunctionType[S] = () => Set(1)
  val creatorFunc = CreatorFunc(create)
  var creatorAgent: CreatorAgent[S] = _

  val mutate: MutatorFunctionType[S] = (set: Set[TScored[S]]) => set.map(_.solution + 1)
  val mutatorFunc = MutatorFunc(mutate)
  var mutatorAgent: MutatorAgent[S] = _

  val delete: DeletorFunctionType[S] = set => set
  val deletorFunc = func.DeletorFunc(delete)
  var deletorAgent: DeletorAgent[S] = _

  var agents: Vector[TAgent[S]] = _
  val mutatorInput: Set[TScored[S]] = Set[TScored[S]](Scored(Map("Score1" -> 3), 2))
  val expectedMessages: Vector[Any] = Vector(
    PopulationActorRef.AddSolutions(create()),
    PopulationActorRef.AddSolutions(mutate(mutatorInput))
    
  )

  before {
    probe = TestProbe()
    pop = PopulationActorRef[S](probe.ref)
    creatorAgent = CreatorAgent(creatorFunc, pop)
    mutatorAgent = MutatorAgent(mutatorFunc, pop)
    deletorAgent = DeletorAgent(deletorFunc, pop)
    agents = Vector(creatorAgent, mutatorAgent, deletorAgent)
    expectedMessages = Vector()
  }




  "All agents" should {
    "step when told to start, stop stepping when told to stop" in {
      for((agent, message) <- agents.zip(agent.start()
      probe.expectMsg(3.seconds, expectedMessage)
      agent.stop()
      probe.expectNoMessage(3.seconds)
    }

    "shouldn't do anything if started twice" in {
      agent.start()
      agent.start()

      agent.stop()
      probe.expectNoMessage(3.seconds)
    }

    "shouldn't break if stopped twice" in {
      agent.start()
      agent.stop()
      agent.stop()
    }
  }
}
