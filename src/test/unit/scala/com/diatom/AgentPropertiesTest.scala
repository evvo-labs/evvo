package com.diatom

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.diatom.agent._
import com.diatom.agent.func.{CreatorFunc, MutatorFunc}
import com.diatom.population.PopulationActorRef
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import scala.collection.mutable
import scala.concurrent.duration._

class AgentPropertiesTest extends TestKit(ActorSystem("AgentPropertiesTest"))
  with WordSpecLike with Matchers with BeforeAndAfter {

  type S = Int

  var probe: TestProbe = _
  var pop: PopulationActorRef[S] = _

  // a mapping from each function to whether it has been called yet
  var agentFunctionCalled: mutable.Map[Any, Boolean] = _

  val create: CreatorFunctionType[S] = () => {
    agentFunctionCalled(create) = true
    Set(1)
  }
  val creatorFunc = CreatorFunc(create)
  var creatorAgent: CreatorAgent[S] = _

  val mutate: MutatorFunctionType[S] = (set: Set[TScored[S]]) => {
    agentFunctionCalled(mutate) = true
    set.map(_.solution + 1)
  }
  val mutatorFunc = MutatorFunc(mutate)
  var mutatorAgent: MutatorAgent[S] = _
  val mutatorInput: Set[TScored[S]] = Set[TScored[S]](Scored(Map("Score1" -> 3), 2))

  val delete: DeletorFunctionType[S] = set => {
    agentFunctionCalled(delete) = true
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
    //    probe.setAutoPilot((sender: ActorRef, msg: Any) => msg match {
    //      case PopulationActorRef.GetSolutions(n) => {
    //        sender ! (Set(Scored(Map("a" -> 3.4), 1)), probe)
    //        TestActor.KeepRunning
    //      }
    //    })

    pop = PopulationActorRef[S](probe.ref)
    creatorAgent = CreatorAgent(creatorFunc, pop)
    mutatorAgent = MutatorAgent(mutatorFunc, pop)
    deletorAgent = DeletorAgent(deletorFunc, pop)
    agents = Vector(creatorAgent, mutatorAgent, deletorAgent)

    agentFunctionCalled = mutable.Map(
      create -> false,
      mutate -> false,
      delete -> false)
  }


  "All agents" should {
    "step when told to start, stop stepping when told to stop" in {
      for (agent <- agents) {
        println(agent)
        agent.start()
        probe.expectMsgType[Any](3.seconds)
        probe.reply(Set(Scored(Map("a" -> 3.4), 1)))
        agent.stop()
      }
      // need to make sure that each of the three core functions have been called,
      // and they have side effects that will turn the maping true
      println(agentFunctionCalled)
      assert(agentFunctionCalled.forall({ case (_, called) => called }))
    }

    "shouldn't do anything if started twice" in {
      for (agent <- agents) {
        agent.start()
        agent.start()

        agent.stop()
        // TODO waits three seconds per agent, figure out how to parallelize
        probe.expectNoMessage(3.seconds)
      }
    }

    "shouldn't break if stopped twice" in {
      for (agent <- agents) {
        agent.start()
        agent.stop()
        agent.stop()
      }
    }
  }
}
