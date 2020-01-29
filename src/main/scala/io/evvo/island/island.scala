package io.evvo.island

import java.util.TimerTask
import java.util.concurrent.{Executors, TimeUnit}

import io.evvo.agent._
import io.evvo.island.population._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/** This component is used to do all the actual work of managing the island, without managing
  * or being tied to where the island is deployed to.
  */
class EvvoIsland[Sol](
    creators: Vector[CreatorFunction[Sol]],
    mutators: Vector[ModifierFunction[Sol]],
    deletors: Vector[DeletorFunction[Sol]],
    fitnesses: Vector[Objective[Sol]],
    immigrationStrategy: ImmigrationStrategy,
    emigrationStrategy: EmigrationStrategy,
    emigrationTargetStrategy: EmigrationTargetStrategy,
    loggingStrategy: LoggingStrategy
) extends EvolutionaryProcess[Sol] {

  private val pop: Population[Sol] = StandardPopulation(fitnesses)
  private val creatorAgents = creators.map(c => CreatorAgent(c, pop))
  private val mutatorAgents = mutators.map(m => ModifierAgent(m, pop))
  private val deletorAgents = deletors.map(d => DeletorAgent(d, pop))
  private val allAgents: Seq[AAgent[Sol]] = creatorAgents ++ mutatorAgents ++ deletorAgents

  /** The list of all other islands, to send emigrating solutions to. */
  private var emigrationTargets: IndexedSeq[EvolutionaryProcess[Sol]] = IndexedSeq()

  /** The index of the current "target" that will receive the next emigration. */
  private var currentEmigrationTargetIndex: Int = 0

//  override def receive: Receive =
//    LoggingReceive({
//      case Run(t) => sender ! this.runBlocking(t)
//      case GetParetoFrontier => sender ! this.currentParetoFrontier()
//      case AddSolutions(solutions) =>
//        Try { solutions.asInstanceOf[Seq[Sol]] }.fold(
//          failure => this.logger.warning(f"Failed receiving AddSolutions message: ${failure}"),
//          this.addSolutions)
//      case Immigrate(solutions) =>
//        Try { solutions.asInstanceOf[Seq[Scored[Sol]]] }.fold(
//          failure => this.logger.warning(f"Failed receiving Immigrate message: ${failure}"),
//          this.immigrate)
//      case RegisterIslands(islands) =>
//        Try { islands.asInstanceOf[Seq[EvolutionaryProcess[Sol]]] }.fold(
//          failure => this.logger.warning(f"Failed receiving RegisterIsland message: ${failure}"),
//          this.registerIslands)
//      case GetAgentStatuses => sender ! this.agentStatuses()
//    })
  override def runAsync(stopAfter: StopAfter): Future[Unit] = {
    allAgents.foreach(_.start())

    val emigrationExecutor = Executors.newSingleThreadScheduledExecutor()
    emigrationExecutor.scheduleAtFixedRate(
      () => this.emigrate(),
      emigrationStrategy.durationBetweenRuns.toMillis,
      emigrationStrategy.durationBetweenRuns.toMillis,
      TimeUnit.MILLISECONDS)

    val done = Promise[Unit]()
    val timer = new java.util.Timer()
    val shutDown = new TimerTask {
      override def run(): Unit = {
        emigrationExecutor.shutdown()
        done.success(())
      }
    }
    timer.schedule(shutDown, stopAfter.time.toMillis)

    done.future
  }

  def runBlocking(stopAfter: StopAfter): Unit = {
    Await.result(this.runAsync(stopAfter), Duration.Inf)
  }

  override def addSolutions(solutions: Seq[Sol]): Unit = {
    pop.addSolutions(solutions)
  }

  override def currentParetoFrontier(): ParetoFrontier[Sol] = {
    pop.getParetoFrontier()
  }

  override def immigrate(solutions: Seq[Scored[Sol]]): Unit = {
    pop.addSolutions(immigrationStrategy.filter(solutions, pop).map(_.solution))
  }

  override def poisonPill(): Unit = {
    stop()
  }

  override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
    emigrationTargets = emigrationTargets ++ islands
  }

  private def stop(): Unit = {
    allAgents.foreach(_.stop())
  }

  private def emigrate(): Unit = {
    if (emigrationTargets.isEmpty) {
      // TODO log
    } else {
      val emigrants = emigrationStrategy.chooseSolutions(this.pop)
      val emigrationTargets = emigrationTargetStrategy.chooseTargets(this.emigrationTargets.length)
      emigrationTargets.foreach(target => {
        this.emigrationTargets(target).immigrate(emigrants)
      })
    }
  }

  override def agentStatuses(): Seq[AgentStatus] = allAgents.map(_.status())
}

object EvvoIsland {

  /** @tparam Sol the type of solutions processed by this island.
    * @return A builder for an EvvoIsland.
    */
  def builder[Sol](): UnfinishedEvvoIslandBuilder[Sol, _, _, _] = EvvoIslandBuilder[Sol]()
}
