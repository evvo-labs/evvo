package com.diatom.agent

import com.diatom._
import com.diatom.agent.func.{CreatorFunc, MutatorFunc}
import com.diatom.population.Population
import com.diatom.tags.Slow
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import scala.collection.mutable
import scala.concurrent.duration._

class AgentPropertiesTest extends WordSpecLike with Matchers with BeforeAndAfter {

  // TODO reimplement this using http://doc.scalatest.org/3.0.1/#org.scalatest.PropSpec@testMatrix

  type S = Int

  var pop: Population[S] = _

  // a mapping from each function to whether it has been called yet
  var agentFunctionCalled: mutable.Map[Any, Boolean] = _

  val create: CreatorFunctionType[S] = () => {
    agentFunctionCalled("create") = true
    Set(1)
  }
  val creatorFunc = CreatorFunc(create, "create")
  var creatorAgent: TAgent[S] = _

  val mutate: MutatorFunctionType[S] = (seq: IndexedSeq[TScored[S]]) => {
    agentFunctionCalled("mutate") = true
    seq.map(_.solution + 1)
  }
  val mutatorFunc = MutatorFunc(mutate, "mutate")
  var mutatorAgent: TAgent[S] = _
  val mutatorInput: Set[TScored[S]] = Set[TScored[S]](Scored(Map("Score1" -> 3), 2))

  val delete: DeletorFunctionType[S] = set => {
    agentFunctionCalled("delete") = true
    set
  }
  val deletorFunc = func.DeletorFunc(delete, "delete", 1)
  var deletorAgent: TAgent[S] = _
  val deletorInput: Set[TScored[S]] = mutatorInput

  var agents: Vector[TAgent[S]] = _
  val strategy: TAgentStrategy = _ => 70.millis

  val fitnessFunc: TFitnessFunc[S] = FitnessFunc(_.toDouble, "Double")

  before {
    pop = Population[S](Vector(fitnessFunc))
    creatorAgent = CreatorAgent.from(creatorFunc, pop, strategy)
    mutatorAgent = MutatorAgent.from(mutatorFunc, pop, strategy)
    deletorAgent = DeletorAgent.from(deletorFunc, pop, strategy)
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
        Thread.sleep(100)
        agent.stop()
      }
      // need to make sure that each of the three core functions have been called,
      // and they have side effects that will turn the mapping true
      assert(agentFunctionCalled.values.reduce(_ && _))
      assert(agents.forall(_.numInvocations > 0))
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

    //    FIXME: This test
    //    "stop when told to" taggedAs Slow in {
    //      for (agent <- agents) {
    //        agent.start()
    //        agent.stop()
    //
    //        // not ideal that this test has to wait three seconds after the agent is stopped,
    //        // but this is the best we can come up with
    //        Thread.sleep(3000)
    //        probe.expectNoMessage(3.seconds)
    //      }
    //    }

    "repeat as often as their strategies say to" taggedAs Slow in {
      for (agent <- agents) {
        agent.start()
        Thread.sleep(100)
        agent.stop()
        Thread.sleep(100)
        // 2 is the ceiling of 100 / 70, we ought to have the agent run twice.
        agent.numInvocations shouldBe 2
      }
    }
  }
}
