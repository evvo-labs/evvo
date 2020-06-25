package io.evvo.migration
import io.evvo.island.population.{ParetoFrontier, Scored}

object closedborders {

  case class DoNothingEmigrator[Sol]() extends Emigrator[Sol] {
    override def emigrate(solutions: Seq[Scored[Sol]]): Unit = {}
  }

  case class DoNothingImmigrator[Sol]() extends Immigrator[Sol] {
    override def immigrate(numberOfImmigrants: Int): Seq[Scored[Sol]] = Seq.empty
  }

  case class DoNotRecord[Sol]() extends ParetoFrontierRecorder[Sol] {
    override def record(pf: ParetoFrontier[Sol]): Unit = {}
  }
}
