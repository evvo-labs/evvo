package io.evvo.migration
import io.evvo.island.EmigrationTargetStrategy
import io.evvo.island.population.Scored

// TODO move to test directory?
// TODO this will break if you have multiple immigrators of different types in one JVM
object local {
  private var immigratorQueues = Map[Immigrator[_], Seq[Scored[_]]]()
  private var immigrators = IndexedSeq[Immigrator[_]]()

  class LocalImmigrator[Sol]() extends Immigrator[Sol] {
    immigratorQueues += (this -> Seq[Scored[Sol]]())

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
          immigratorQueues
            .updated(immigrators(index), immigratorQueues(immigrators(index)) ++ solutions))
    }
  }
}
