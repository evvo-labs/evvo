package com.diatom.population

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.testkit.TestKit
import akka.util.Timeout
import com.diatom.population.PopulationActorRef.{DeleteSolutions, GetSolutions}
import com.diatom.{Scored, agent}
import org.scalatest.{Assertion, AsyncWordSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.Future

class PopulationTest extends TestKit(ActorSystem("PopulationTest"))
  with AsyncWordSpecLike with Matchers {
  val identityFitness = agent.FitnessFunc[Double](x => x.toDouble)
  val fitnesses = Set(identityFitness)
  implicit val timeout = Timeout(1.second)


  def getSolutions(pop: ActorRef, n: Int): Future[Set[Double]] = {
    (pop ? PopulationActorRef.GetSolutions(1)).asInstanceOf[Future[Set[Double]]]
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
  }

  "A non-empty population" should {
    // TODO non-empty population tests
  }
}
