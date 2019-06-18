package com.evvo.integration

import com.evvo.NullLogger
import com.evvo.agent.defaults.DeleteWorstHalfByRandomObjective
import com.evvo.agent.{CreatorFunction, MutatorFunction}
import com.evvo.integration.LocalEvvoTestFixtures.{Creator, Mutator, NumInversions, Solution}
import com.evvo.island.population.{Minimize, Objective, Scored}
import com.evvo.island.{EvolutionaryProcess, EvvoIslandBuilder, StopAfter, UnfinishedEvvoIslandBuilder}
import com.evvo.island.population.{Minimize, Objective}
import com.evvo.tags.{Performance, Slow}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

/**
  * Tests a single island cluster.
  *
  * The behavior under test is that an Island can sort a list given the proper modifier and fitness
  * function, and terminate successfully returning a set of lists.
  */
class LocalEvvoTest extends WordSpec with Matchers {

  /**
    * Creates a test Evvo instance running locally, that will use basic swapping
    * to mutate lists, starting in reverse order, scoring them on the number of inversions.
    *
    * @param listLength the length of the lists to sort.
    * @return
    */
  def getEvvo(listLength: Int): EvolutionaryProcess[Solution] = {

    val islandBuilder = EvvoIslandBuilder[Solution]()
      .addCreator(new Creator(listLength))
      .addModifier(new Mutator())
      .addDeletor(DeleteWorstHalfByRandomObjective())
      .addObjective(new NumInversions())

    islandBuilder.buildLocalEvvo()
  }

  implicit val log = NullLogger

  "Local Evvo" should {
    val timeout = 1
    val listLength = 6
    f"be able to sort a list of length $listLength within $timeout seconds" taggedAs(Performance, Slow) in {
      val terminate = StopAfter(timeout.seconds)

      val evvo = getEvvo(listLength)
      evvo.runBlocking(terminate)
      val pareto: Set[Solution] = evvo
        .currentParetoFrontier()
        .solutions
        .map(_.solution)
      pareto should contain(1 to listLength toList)
      pareto.size shouldBe 1
    }
  }
}


object LocalEvvoTestFixtures {
  type Solution = List[Int]

  class Creator(listLength: Int) extends CreatorFunction[Solution]("Creator") {
    override def create(): TraversableOnce[Solution] = {
      Vector((listLength to 1 by -1).toList)
    }
  }

  class Mutator extends MutatorFunction[Solution]("Modifier") {
    override def mutate(sol: Solution): Solution = {
      val i = util.Random.nextInt(sol.length)
      val j = util.Random.nextInt(sol.length)
      val tmp = sol(j)
      sol.updated(j, sol(i)).updated(i, tmp)
    }
  }

  class NumInversions extends Objective[Solution]("Inversions", Minimize) {
    override protected def objective(sol: Solution): Double = {
      (for ((elem, index) <- sol.zipWithIndex) yield {
        sol.drop(index).count(_ < elem)
      }).sum
    }
  }
}
