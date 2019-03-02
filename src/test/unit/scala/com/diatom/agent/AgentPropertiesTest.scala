package com.diatom.agent

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.diatom._
import com.diatom.agent.func.{CreatorFunc, MutatorFunc}
import com.diatom.population.PopulationActorRef
import com.diatom.tags.Slow
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import scala.collection.mutable
import scala.concurrent.duration._

class AgentPropertiesTest extends TestKit(ActorSystem("AgentPropertiesTest"))
  with WordSpecLike with Matchers with BeforeAndAfter {

  // TODO reimplement this using http://doc.scalatest.org/3.0.1/#org.scalatest.PropSpec@testMatrix

  type S = Int

  var probe: TestProbe = _
  var pop: PopulationActorRef[S] = _

  // a mapping from each function to whether it has been called yet
  var agentFunctionCalled: mutable.Map[Any, Boolean] = _

  val create: CreatorFunctionType[S] = () => {
    agentFunctionCalled("create") = true
    Set(1)
  }
  val creatorFunc = CreatorFunc(create)
  var creatorAgent: CreatorAgent[S] = _

  val mutate: MutatorFunctionType[S] = (set: Set[TScored[S]]) => {
    agentFunctionCalled("mutate") = true
    set.map(_.solution + 1)
  }
  val mutatorFunc = MutatorFunc(mutate)
  var mutatorAgent: MutatorAgent[S] = _
  val mutatorInput: Set[TScored[S]] = Set[TScored[S]](Scored(Map("Score1" -> 3), 2))

  val delete: DeletorFunctionType[S] = set => {
    agentFunctionCalled("delete") = true
    set
  }
  val deletorFunc = func.DeletorFunc(delete)
  var deletorAgent: DeletorAgent[S] = _
  val deletorInput: Set[TScored[S]] = mutatorInput

  var agents: Vector[TAgent[S]] = _


  //  val expectedMessages: Vector[Any] = Vector(
  //    PopulationActorRef.AddSolutions(create()),
  //    PopulationActorRef.AddSolutions(mutate(mutatorInput)),
  //    PopulationActorRef.DeleteSolutions(delete(deletorInput))
  //  )

  before {
    probe = TestProbe()

    pop = PopulationActorRef[S](probe.ref)
    creatorAgent = CreatorAgent(creatorFunc, pop)
    mutatorAgent = MutatorAgent(mutatorFunc, pop)
    deletorAgent = DeletorAgent(deletorFunc, pop)
    agents = Vector(creatorAgent, mutatorAgent, deletorAgent)

    agentFunctionCalled = mutable.Map(
      "create" -> false,
      "mutate" -> false,
      "delete" -> false)
  }


  "All agents" should {
    "step when told to start, stop stepping when told to stop" taggedAs Slow in {
      for (agent <- agents) {
        agent.start()
        probe.expectMsgType[Any](3.seconds)
        probe.reply(Set(Scored(Map("a" -> 3.4), 1)))
        probe.expectMsgType[Any](3.seconds)
        agent.stop()
      }
      // need to make sure that each of the three core functions have been called,
      // and they have side effects that will turn the maping true
      println(agentFunctionCalled)
      assert(agentFunctionCalled.values.reduce(_ && _))
    }

    "not do anything if started twice" in {
      for (agent <- agents) {
        agent.start()
        agent.start()

        agent.stop()
      }
    }

    "not break if stopped twice" in {
      for (agent <- agents) {
        agent.start()
        agent.stop()
        agent.stop()
      }
    }

    "stop when told to" taggedAs Slow in {
      for (agent <- agents) {
        agent.start()
        agent.stop()

        // not ideal that this test has to wait three seconds after the agent is stopped,
        // but this is the best we can come up with
        Thread.sleep(3000)
        probe.expectNoMessage(3.seconds)
      }
    }
  }
}
