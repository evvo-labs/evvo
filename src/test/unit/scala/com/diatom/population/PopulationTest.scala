package com.diatom.population

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.testkit.TestKit
import akka.util.Timeout
import com.diatom.agent.TPopulationInformation
import com.diatom.population.PopulationActorRef.{DeleteSolutions, GetParetoFrontier, GetSolutions}
import com.diatom.{Scored, TParetoFrontier, TScored, agent}
import org.scalatest.{Assertion, AsyncWordSpecLike, BeforeAndAfter, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PopulationTest extends TestKit(ActorSystem("PopulationTest"))
  with AsyncWordSpecLike with Matchers with BeforeAndAfter {
  val identityFitness = agent.FitnessFunc[Double](x => x.toDouble)
  val fitnesses = Set(identityFitness)
  implicit val timeout = Timeout(1.second)


  def getSolutions(pop: ActorRef, n: Int): Future[Set[TScored[Double]]] = {
    (pop ? PopulationActorRef.GetSolutions(n)).asInstanceOf[Future[Set[TScored[Double]]]]
  }

  "An empty population" should {
    val emptyPop: ActorRef = Population.from(fitnesses)

    "not return any solutions" in {
      val sols = getSolutions(emptyPop, 1)
      sols.map(_ shouldBe 'empty)
    }

    "not do anything when deleted from" in {
      emptyPop ! PopulationActorRef.DeleteSolutions(Seq(Scored(Map("a" -> 1.0), 1.0)))

      val sols = getSolutions(emptyPop, 1)
      sols.map(_ shouldBe 'empty)
    }

    "become non-empty when added to" in {
      val pop = Population.from(fitnesses)
      pop ! PopulationActorRef.AddSolutions(Set(1.0))
      val sols = getSolutions(pop, 1)
      sols.map(_ should not be 'empty)
    }

    "have an empty pareto frontier" in {
      val paretoFront = (emptyPop ? PopulationActorRef.GetParetoFrontier)
      .asInstanceOf[Future[TParetoFrontier[Double]]]
      paretoFront.map(_.solutions shouldBe 'empty)
    }

    "has zero elements, according to getInformation()" in {
      val info = (emptyPop ? PopulationActorRef.GetInformation)
        .asInstanceOf[Future[TPopulationInformation]]
      info.map(_.numSolutions shouldBe 0)
    }
  }

  "A non-empty population" should {
    var pop: ActorRef = Population.from(fitnesses)
    val popSize = 10
    before {
      pop = Population.from(fitnesses)
      pop ! PopulationActorRef.AddSolutions((1 to popSize).map(_.toDouble))
    }

    "not contain elements after the elements are removed" in {
      val sol = Await.result(getSolutions(pop, 1), 1.second)
      pop ! PopulationActorRef.DeleteSolutions(sol)
      val remaining = getSolutions(pop, popSize)
      remaining.map(_ & sol shouldBe 'empty)
    }

    "only remove the specified elements" in {
      val before = Await.result(getSolutions(pop, popSize), 1.second)
      val sol = Await.result(getSolutions(pop, 1), 1.second)
      pop ! PopulationActorRef.DeleteSolutions(sol)
      val after = getSolutions(pop, popSize)
      after.map(before &~ _ shouldBe sol)
    }

    "return random subsections of the population" in {
      val multipleSolSelections: List[Set[TScored[Double]]] =
        List.fill(12)(Await.result(getSolutions(pop, popSize / 2), 1.second))
      assert(multipleSolSelections.toSet.size > 1)
    }

    "allow adding elements" in {
      pop ! PopulationActorRef.AddSolutions(Set(11.0))
      val sol = getSolutions(pop, popSize + 1)
      sol.map(_.map(_.solution) should contain(11.0))
    }

    "not allow duplicates" in {
      pop ! PopulationActorRef.AddSolutions(Set(10.0))
      val sol = getSolutions(pop, popSize + 1)
      sol.map(_.size shouldBe popSize)
    }

    "have only one element in a one-dimensional pareto frontier" in {
      val p = (pop ? PopulationActorRef.GetParetoFrontier)
        .asInstanceOf[Future[TParetoFrontier[Double]]]
      p.map(_.solutions.size shouldBe 1)
    }

    "have non-zero number of elements, according to getInformation()" in {
      val info = (pop ? PopulationActorRef.GetInformation)
        .asInstanceOf[Future[TPopulationInformation]]
      info.map(_.numSolutions shouldBe 10)
    }
  }
}
