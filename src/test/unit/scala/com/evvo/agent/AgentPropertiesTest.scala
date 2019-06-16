package com.evvo.agent

import akka.event.LoggingAdapter
import com.evvo._
import com.evvo.island.population.{Minimize, Objective, Scored, StandardPopulation}
import com.evvo.tags.Slow
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import scala.collection.mutable
import scala.concurrent.duration._

class AgentPropertiesTest extends WordSpecLike with Matchers with BeforeAndAfter {

  implicit val log: LoggingAdapter = NullLogger

  // TODO reimplement this using http://doc.scalatest.org/3.0.1/#org.scalatest.PropSpec@testMatrix

  type S = Int

  var pop: StandardPopulation[S] = _

  // a mapping from each function to whether it has been called yet
  var agentFunctionCalled: mutable.Map[Any, Boolean] = _

  val creatorFunc = new CreatorFunction[S]("creator") {
    override def create(): TraversableOnce[S] = {
      agentFunctionCalled("create") = true
      Vector(1)
    }
  }

  var creatorAgent: Agent[S] = _

  val mutatorFunc = new ModifierFunction[S]("mutator") {
    def modify(seq: IndexedSeq[Scored[S]]): IndexedSeq[S] = {
      agentFunctionCalled("mutate") = true
      seq.map(_.solution + 1)
    }
  }

  var mutatorAgent: Agent[S] = _
  val mutatorInput: Set[Scored[S]] = Set[Scored[S]](Scored(Map(("Score1", Minimize) -> 3), 2))

  val deletorFunc = new DeletorFunction[S]("deletor") {
    override def delete(sols: IndexedSeq[Scored[S]]): TraversableOnce[Scored[S]] = {
      agentFunctionCalled("delete") = true
      sols
    }
  }

  var deletorAgent: Agent[S] = _
  val deletorInput: Set[Scored[S]] = mutatorInput

  var agents: Vector[Agent[S]] = _
  val strategy: AgentStrategy = _ => 70.millis

  val fitnessFunc: Objective[S] = new Objective[S]("Double", Minimize) {
    override protected def objective(sol: S): Double = sol.toDouble
  }

  before {
    pop = StandardPopulation[S](Vector(fitnessFunc))
    creatorAgent = CreatorAgent(creatorFunc, pop, strategy)
    mutatorAgent = ModifierAgent(mutatorFunc, pop, strategy)
    deletorAgent = DeletorAgent(deletorFunc, pop, strategy)
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
