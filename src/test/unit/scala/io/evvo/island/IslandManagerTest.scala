package io.evvo.island

import io.evvo.agent.{CreatorFunction, ModifierFunction}
import io.evvo.builtin.bitstrings.Bitstring
import io.evvo.builtin.deletors.DeleteDominated
import io.evvo.island.population._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class IslandManagerTest extends WordSpec with Matchers {

  case object NoCreator extends CreatorFunction[Bitstring]("no") {
    override def create(): Iterable[Bitstring] = Seq()
  }
  case object NoModifier extends ModifierFunction[Bitstring]("No") {
    override def modify(sols: IndexedSeq[Scored[Bitstring]]): Iterable[Bitstring] = Seq()
  }

  case object OneMax extends Objective[Bitstring]("OneMax", Maximize) {
    override protected def objective(sol: Bitstring): Double = sol.count(identity)
  }

  case object ZeroMax extends Objective[Bitstring]("ZeroMax", Maximize) {
    override protected def objective(sol: Bitstring): Double = sol.length - sol.count(identity)
  }

  // Make sure they aren't creating any solutions
  val builderOneMax = EvvoIslandBuilder[Bitstring]()
    .addCreator(NoCreator)
    .addModifier(NoModifier)
    .addDeletor(DeleteDominated())
    .addObjective(OneMax)
    .withEmigrationStrategy(WholeParetoFrontierEmigrationStrategy())
    .withEmigrationTargetStrategy(SendToAllEmigrationTargetStrategy())

  val builderZeroAndOneMax =  builderOneMax.addObjective(ZeroMax)

  "A Local Island Manager" should {
    "Use the provided network topology to register islands with each other" in {

      // Expression blocks so we can reuse the names island1, etc.
      val _ = {
        val island1 = builderOneMax.buildLocalEvvo()
        val island2 = builderOneMax.buildLocalEvvo()
        val island3 = builderOneMax.buildLocalEvvo()

        // simply constructing the manager registers the islands with each other
        val mgr = new IslandManager[Bitstring](
          Seq(island1, island2, island3),
          FullyConnectedNetworkTopology())
        island1.immigrate(Seq(Scored(Map(), Seq(true))))

        mgr.runBlocking(StopAfter(1.second))
        island2.currentParetoFrontier().solutions should have size 1
        island3.currentParetoFrontier().solutions should have size 1
      }

      case object NoTopology extends NetworkTopology {
        override def configure(numIslands: Int): Seq[Connection] = Seq()
      }

      val _ = {
        val island1 = builderOneMax.buildLocalEvvo()
        val island2 = builderOneMax.buildLocalEvvo()
        val mgr =
          new IslandManager[Bitstring](Seq(island1, island2), NoTopology)
        island1.immigrate(Seq(Scored(Map(), Seq(true))))
        mgr.runBlocking(StopAfter(1.second))

        island2.currentParetoFrontier().solutions should have size 0
      }
    }

    "Be able to distribute a population among the islands" in {
      // Add solutions to population, each of which would lie on the pareto frontier of empty pop
      val solutionsToAdd: Seq[Bitstring] = (0 to 30) // 30 BitStrings
        .map(bs => (0 until 30) // each of length 30
          .map(i => if (i <= bs) true else false)) // Containing unique number of trues

      // Expression blocks so we can resuse the names island1, etc.
      val _ = {
        val island1 = builderZeroAndOneMax.buildLocalEvvo()
        val island2 = builderZeroAndOneMax.buildLocalEvvo()
        val island3 = builderZeroAndOneMax.buildLocalEvvo()

        // simply constructing the manager registers the islands with each other
        val mgr = new IslandManager[Bitstring](
          Seq(island1, island2, island3),
          FullyConnectedNetworkTopology())

        // All islands have no population
        island1.currentParetoFrontier().solutions should have size 0
        island2.currentParetoFrontier().solutions should have size 0
        island3.currentParetoFrontier().solutions should have size 0

        mgr.addSolutions(solutionsToAdd)

        // All islands got 30/3 new items (each solution happens to be non-dominated in test)
        island1.currentParetoFrontier().solutions should have size 10
        island2.currentParetoFrontier().solutions should have size 10
        island3.currentParetoFrontier().solutions should have size 10

        mgr.currentParetoFrontier().solutions should have size 30
      }

      val _ = {
        val island1 = builderZeroAndOneMax.buildLocalEvvo()
        val island2 = builderZeroAndOneMax.buildLocalEvvo()
        val island3 = builderZeroAndOneMax.buildLocalEvvo()
        val island4 = builderZeroAndOneMax.buildLocalEvvo()

        // simply constructing the manager registers the islands with each other
        val mgr = new IslandManager[Bitstring](
          Seq(island1, island2, island3, island4),
          FullyConnectedNetworkTopology())

        mgr.addSolutions(solutionsToAdd)

        // All islands got some new items (each solution happens to be non-dominated in test)
        // No promises that distribution is equal
        island1.currentParetoFrontier().solutions should not be empty
        island2.currentParetoFrontier().solutions should not be empty
        island3.currentParetoFrontier().solutions should not be empty
        island4.currentParetoFrontier().solutions should not be empty

        mgr.currentParetoFrontier().solutions should have size 30
      }

      val _ = {
        val island1 = builderZeroAndOneMax.buildLocalEvvo()
        val island2 = builderZeroAndOneMax.buildLocalEvvo()
        val island3 = builderZeroAndOneMax.buildLocalEvvo()
        val island4 = builderZeroAndOneMax.buildLocalEvvo()

        // simply constructing the manager registers the islands with each other
        val mgr = new IslandManager[Bitstring](
          Seq(island1, island2, island3, island4),
          FullyConnectedNetworkTopology())

        mgr.currentParetoFrontier().solutions should have size 0

        mgr.addSolutions(Seq(Seq(true)))

        mgr.currentParetoFrontier().solutions should have size 1
      }
    }
  }
}
