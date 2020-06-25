package io.evvo.island

import io.evvo.agent.{CreatorFunction, DeletorFunction, ModifierFunction}
import io.evvo.island.EvvoIslandBuilder.HAS_SOME
import io.evvo.island.population.Objective
import io.evvo.migration.{Emigrator, Immigrator, ParetoFrontierRecorder}

/** <p>
  * A builder for `Local` and `RemoteEvvoIsland`s. See
  * [[http://blog.rafaelferreira.net/2008/07/type-safe-builder-pattern-in-scala.html]]
  * for an explanation of the type parameters. Users should never interact with this class directly,
  * instead getting a result from `EvvoIsland.builder()` or `EvvoIslandBuilder()`, adding the
  * required data, and then implicitly converting to a `FinishedEvvoIslandBuilder`.
  * </p>
  *
  * <p>
  * Can be implicitly converted to a [[io.evvo.island.FinishedEvvoIslandBuilder]] once it
  * has had at least one creator, one mutator, one deletor, and one objective added.
  * </p>
  *
  * @param creators            The functions to be used for creating new solutions.
  * @param modifiers           The functions to be used for creating new solutions from current solutions.
  * @param deletors            The functions to be used for deciding which solutions to delete.
  * @param fitnesses          The functions to maximize.
  * @param immigrator Used to determine which solutions to accept during immigration.
  * @tparam HasModifiers  same as HasCreators
  * @tparam HasDeletors   same as HasCreators
  * @tparam HasFitnesses  same as HasCreators
  */
case class UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HasDeletors,
    HasFitnesses,
    HasImmigrator,
    HasImmigrationStrategy,
    HasEmigrator,
    HasEmigrationStrategy,
    HasLoggingStrategy,
    HasParetoFrontierRecorder](
    creators: Set[CreatorFunction[Sol]] = Set[CreatorFunction[Sol]](),
    modifiers: Set[ModifierFunction[Sol]] = Set[ModifierFunction[Sol]](),
    deletors: Set[DeletorFunction[Sol]] = Set[DeletorFunction[Sol]](),
    fitnesses: Set[Objective[Sol]] = Set[Objective[Sol]](),
    immigrator: Option[Immigrator[Sol]] = None,
    immigrationStrategy: Option[ImmigrationStrategy] = None,
    emigrator: Option[Emigrator[Sol]] = None,
    emigrationStrategy: Option[EmigrationStrategy] = None,
    loggingStrategy: LoggingStrategy = LogPopulationLoggingStrategy(),
    paretoFrontierRecorder: Option[ParetoFrontierRecorder[Sol]] = None) {

  def addCreator(creatorFunc: CreatorFunction[Sol]): UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HasDeletors,
    HasFitnesses,
    HasImmigrator,
    HasImmigrationStrategy,
    HasEmigrator,
    HasEmigrationStrategy,
    HasLoggingStrategy,
    HasParetoFrontierRecorder] = {
    this.copy(creators = creators + creatorFunc)
  }

  def addModifier(modifierFunc: ModifierFunction[Sol]): UnfinishedEvvoIslandBuilder[
    Sol,
    HAS_SOME,
    HasDeletors,
    HasFitnesses,
    HasImmigrator,
    HasImmigrationStrategy,
    HasEmigrator,
    HasEmigrationStrategy,
    HasLoggingStrategy,
    HasParetoFrontierRecorder] = {
    this.copy(modifiers = modifiers + modifierFunc)
  }

  def addDeletor(deletorFunc: DeletorFunction[Sol]): UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HAS_SOME,
    HasFitnesses,
    HasImmigrator,
    HasImmigrationStrategy,
    HasEmigrator,
    HasEmigrationStrategy,
    HasLoggingStrategy,
    HasParetoFrontierRecorder] = {
    this.copy(deletors = deletors + deletorFunc)
  }

  def addObjective(objective: Objective[Sol]): UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HasDeletors,
    HAS_SOME,
    HasImmigrator,
    HasImmigrationStrategy,
    HasEmigrator,
    HasEmigrationStrategy,
    HasLoggingStrategy,
    HasParetoFrontierRecorder] = {
    this.copy(fitnesses = fitnesses + objective)
  }

  def withImmigrator(immigrator: Immigrator[Sol]): UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HasDeletors,
    HasFitnesses,
    HAS_SOME,
    HasImmigrationStrategy,
    HasEmigrator,
    HasEmigrationStrategy,
    HasLoggingStrategy,
    HasParetoFrontierRecorder] = {
    this.copy(immigrator = Some(immigrator))
  }

  def withImmigrationStrategy(
      immigrationStrategy: ImmigrationStrategy): UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HasDeletors,
    HasFitnesses,
    HasImmigrator,
    HAS_SOME,
    HasEmigrator,
    HasEmigrationStrategy,
    HasLoggingStrategy,
    HasParetoFrontierRecorder] = {
    this.copy(immigrationStrategy = Some(immigrationStrategy))
  }

  def withEmigrator(emigrator: Emigrator[Sol]): UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HasDeletors,
    HasFitnesses,
    HasImmigrator,
    HasImmigrationStrategy,
    HAS_SOME,
    HasEmigrationStrategy,
    HasLoggingStrategy,
    HasParetoFrontierRecorder] = {
    this.copy(emigrator = Some(emigrator))
  }

  def withEmigrationStrategy(emigrationStrategy: EmigrationStrategy): UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HasDeletors,
    HasFitnesses,
    HasImmigrator,
    HasImmigrationStrategy,
    HasEmigrator,
    HAS_SOME,
    HasLoggingStrategy,
    HasParetoFrontierRecorder] = {
    this.copy(emigrationStrategy = Some(emigrationStrategy))
  }

  def withLoggingStrategy(loggingStrategy: LoggingStrategy): UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HasDeletors,
    HasFitnesses,
    HasImmigrator,
    HasImmigrationStrategy,
    HasEmigrator,
    HasEmigrationStrategy,
    HAS_SOME,
    HasParetoFrontierRecorder] = {
    this.copy(loggingStrategy = loggingStrategy)
  }

  def withParetoFrontierRecorder(
      paretoFrontierRecorder: ParetoFrontierRecorder[Sol]): UnfinishedEvvoIslandBuilder[
    Sol,
    HasModifiers,
    HasDeletors,
    HasFitnesses,
    HasImmigrator,
    HasImmigrationStrategy,
    HasEmigrator,
    HasEmigrationStrategy,
    HasLoggingStrategy,
    HAS_SOME] = {
    this.copy(paretoFrontierRecorder = Some(paretoFrontierRecorder))
  }
}

/** A finished EvvoIslandBuilder can build an actual island. See
  * [[io.evvo.island.UnfinishedEvvoIslandBuilder]] for more information.
  */
case class FinishedEvvoIslandBuilder[Sol: Manifest](
    creators: Set[CreatorFunction[Sol]],
    modifiers: Set[ModifierFunction[Sol]],
    deletors: Set[DeletorFunction[Sol]],
    objectives: Set[Objective[Sol]],
    immigrator: Immigrator[Sol],
    immigrationStrategy: ImmigrationStrategy,
    emigrator: Emigrator[Sol],
    emigrationStrategy: EmigrationStrategy,
    loggingStrategy: LoggingStrategy,
    paretoFrontierRecorder: ParetoFrontierRecorder[Sol]
) {
  assert(modifiers.nonEmpty)
  assert(deletors.nonEmpty)
  assert(objectives.nonEmpty)

  def build(): EvolutionaryProcess[Sol] = {
    new EvvoIsland[Sol](
      creators.toVector,
      modifiers.toVector,
      deletors.toVector,
      objectives.toVector,
      immigrator,
      immigrationStrategy,
      emigrator,
      emigrationStrategy,
      loggingStrategy,
      paretoFrontierRecorder
    )
  }
}

object EvvoIslandBuilder {

  /** @return a new, unfinished EvvoIslandBuilder. */
  def apply[Sol: Manifest](): UnfinishedEvvoIslandBuilder[
    Sol,
    HAS_NONE,
    HAS_NONE,
    HAS_NONE,
    HAS_NONE,
    HAS_NONE,
    HAS_NONE,
    HAS_NONE,
    HAS_NONE,
    HAS_NONE] = {
    UnfinishedEvvoIslandBuilder()
  }

  /** Implicitly converts a builder with all required fields into a Finished builder.
    *
    * @param builder the builder to convert
    * @return A FinishedEvvoIslandBuilder with the same data as `builder`
    */
  implicit def finishBuilder[Sol: Manifest](
      builder: UnfinishedEvvoIslandBuilder[
        Sol,
        HAS_SOME,
        HAS_SOME,
        HAS_SOME,
        HAS_SOME,
        HAS_SOME,
        HAS_SOME,
        HAS_SOME,
        HAS_SOME,
        HAS_SOME]
  ): FinishedEvvoIslandBuilder[Sol] = {
    FinishedEvvoIslandBuilder(
      builder.creators,
      builder.modifiers,
      builder.deletors,
      builder.fitnesses,
      builder.immigrator.get,
      builder.immigrationStrategy.get,
      builder.emigrator.get,
      builder.emigrationStrategy.get,
      builder.loggingStrategy,
      builder.paretoFrontierRecorder.get
    )
  }

  // Whether the builder has something or not, used in type parameters.
  abstract class HAS_SOME
  abstract class HAS_NONE
}
