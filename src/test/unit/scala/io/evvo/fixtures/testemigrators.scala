package unit.scala.io.evvo.fixtures

import io.evvo.island.EmigrationTargetStrategy
import io.evvo.island.population.{ParetoFrontier, Scored}
import io.evvo.migration.{Emigrator, Immigrator, ParetoFrontierRecorder}

object testemigrators {
  var immigratorQueues = Map[Immigrator[_], Seq[Scored[_]]]()
  var immigrators = IndexedSeq[Immigrator[_]]()

  class LocalImmigrator[Sol]() extends Immigrator[Sol] {
    immigrators +:= this
    immigratorQueues = immigratorQueues + (this -> Seq[Scored[Sol]]())

    override def immigrate(numberOfImmigrants: Int): Seq[Scored[Sol]] = {
      val result = immigratorQueues(this).take(numberOfImmigrants)
      immigratorQueues.updated(this, immigratorQueues(this).drop(numberOfImmigrants))
      result.asInstanceOf[Seq[Scored[Sol]]]
    }
  }

  class LocalEmigrator[Sol](targetStrategy: EmigrationTargetStrategy) extends Emigrator[Sol] {

    override def emigrate(solutions: Seq[Scored[Sol]]): Unit = {
      val targets = targetStrategy.chooseTargets(immigrators.length)
      targets.foreach(
        index =>
          immigratorQueues = immigratorQueues
            .updated(immigrators(index), immigratorQueues(immigrators(index)) ++ solutions))
    }
  }

  def reset(): Unit = {
    this.immigratorQueues = Map()
    this.immigrators = IndexedSeq()
  }

  case class LocalParetoFrontierIgnorer[Sol]() extends ParetoFrontierRecorder[Sol] {
    override def record(pf: ParetoFrontier[Sol]): Unit = {}
  }
}
