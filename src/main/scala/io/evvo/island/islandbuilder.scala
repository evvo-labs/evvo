// TODO fix builders
//package io.evvo.island
//
//import io.evvo.agent.{CreatorFunction, DeletorFunction, ModifierFunction}
//import io.evvo.island.EvvoIslandBuilder.HAS_SOME
//import io.evvo.island.population.Objective
//import io.evvo.migration.{Emigrator, Immigrator}
//
///** <p>
//  * A builder for `Local` and `RemoteEvvoIsland`s. See
//  * [[http://blog.rafaelferreira.net/2008/07/type-safe-builder-pattern-in-scala.html]]
//  * for an explanation of the type parameters. Users should never interact with this class directly,
//  * instead getting a result from `EvvoIsland.builder()` or `EvvoIslandBuilder()`, adding the
//  * required data, and then implicitly converting to a `FinishedEvvoIslandBuilder`.
//  * </p>
//  *
//  * <p>
//  * Can be implicitly converted to a [[io.evvo.island.FinishedEvvoIslandBuilder]] once it
//  * has had at least one creator, one mutator, one deletor, and one objective added.
//  * </p>
//  *
//  * @param creators            The functions to be used for creating new solutions.
//  * @param modifiers           The functions to be used for creating new solutions from current solutions.
//  * @param deletors            The functions to be used for deciding which solutions to delete.
//  * @param objectives          The functions to maximize.
//  * @param immigrator Used to determine which solutions to accept during immigration.
//  * @tparam HasModifiers  same as HasCreators
//  * @tparam HasDeletors   same as HasCreators
//  * @tparam HasObjectives same as HasCreators
//  */
//case class UnfinishedEvvoIslandBuilder[Sol, HasModifiers, HasDeletors, HasObjectives, HasImmigrator, HasEmigrator](
//                                                                                       creators: Set[CreatorFunction[Sol]] = Set[CreatorFunction[Sol]](),
//                                                                                       modifiers: Set[ModifierFunction[Sol]] = Set[ModifierFunction[Sol]](),
//                                                                                       deletors: Set[DeletorFunction[Sol]] = Set[DeletorFunction[Sol]](),
//                                                                                       objectives: Set[Objective[Sol]] = Set[Objective[Sol]](),
//                                                                                       immigrator: Immigrator = Immigrator(AllowAllImmigrationStrategy()),
//                                                                                       emigrator: Emigrator = RandomSampleEmigrationStrategy(4),
//                                                                                       loggingStrategy: LoggingStrategy = LogPopulationLoggingStrategy()
//) {
//  def addCreator(creatorFunc: CreatorFunction[Sol])
//    : UnfinishedEvvoIslandBuilder[Sol, HasModifiers, HasDeletors, HasObjectives] = {
//    this.copy(creators = creators + creatorFunc)
//  }
//
//  def addModifier(modifierFunc: ModifierFunction[Sol])
//    : UnfinishedEvvoIslandBuilder[Sol, HAS_SOME, HasDeletors, HasObjectives] = {
//    this.copy(modifiers = modifiers + modifierFunc)
//  }
//
//  def addDeletor(deletorFunc: DeletorFunction[Sol])
//    : UnfinishedEvvoIslandBuilder[Sol, HasModifiers, HAS_SOME, HasObjectives] = {
//    this.copy(deletors = deletors + deletorFunc)
//  }
//
//  def addObjective(objective: Objective[Sol])
//    : UnfinishedEvvoIslandBuilder[Sol, HasModifiers, HasDeletors, HAS_SOME] = {
//    this.copy(objectives = objectives + objective)
//  }
//
//  def withImmigrator(immigrator: Immigration)
//    : UnfinishedEvvoIslandBuilder[Sol, HasModifiers, HasDeletors, HasObjectives] = {
//    this.copy(immigrator = immigrator)
//  }
//
//  def withEmigrator(emigrator: Emigrator)
//    : UnfinishedEvvoIslandBuilder[Sol, HasModifiers, HasDeletors, HasObjectives] = {
//    this.copy(emigrator = emigrator)
//  }
//
//  def withEmigrationTargetStrategy(emigrationTargetStrategy: EmigrationTargetStrategy)
//    : UnfinishedEvvoIslandBuilder[Sol, HasModifiers, HasDeletors, HasObjectives] = {
//    this.copy(emigrationTargetStrategy = emigrationTargetStrategy)
//  }
//
//  def withLoggingStrategy(loggingStrategy: LoggingStrategy)
//    : UnfinishedEvvoIslandBuilder[Sol, HasModifiers, HasDeletors, HasObjectives] = {
//    this.copy(loggingStrategy = loggingStrategy)
//  }
//}
//
///** A finished EvvoIslandBuilder can build an actual island. See
//  * [[io.evvo.island.UnfinishedEvvoIslandBuilder]] for more information.
//  */
//case class FinishedEvvoIslandBuilder[Sol](
//    creators: Set[CreatorFunction[Sol]],
//    modifiers: Set[ModifierFunction[Sol]],
//    deletors: Set[DeletorFunction[Sol]],
//    objectives: Set[Objective[Sol]],
//    immigrator: Immigrator[Sol],
//    emigrator: Emigrator[Sol],
//    loggingStrategy: LoggingStrategy
//) {
//  assert(modifiers.nonEmpty)
//  assert(deletors.nonEmpty)
//  assert(objectives.nonEmpty)
//
//  def build(): EvolutionaryProcess[Sol] = {
//    new EvvoIsland[Sol](
//      creators.toVector,
//      modifiers.toVector,
//      deletors.toVector,
//      objectives.toVector,
//      immigrator,
//      emigrator,
//      loggingStrategy
//    )
//  }
//}
//
//object EvvoIslandBuilder {
//
//  /** @return a new, unfinished EvvoIslandBuilder. */
//  def apply[Sol](): UnfinishedEvvoIslandBuilder[Sol, HAS_NONE, HAS_NONE, HAS_NONE] = {
//    UnfinishedEvvoIslandBuilder()
//  }
//
//  /** Implicitly converts a builder with all required fields into a Finished builder.
//    *
//    * @param builder the builder to convert
//    * @return A FinishedEvvoIslandBuilder with the same data as `builder`
//    */
//  implicit def finishBuilder[Sol](
//      builder: UnfinishedEvvoIslandBuilder[Sol, HAS_SOME, HAS_SOME, HAS_SOME]
//  ): FinishedEvvoIslandBuilder[Sol] = {
//    FinishedEvvoIslandBuilder(
//      builder.creators,
//      builder.modifiers,
//      builder.deletors,
//      builder.objectives,
//      builder.immigrator,
//      builder.emigrator,
//      builder.emigrationTargetStrategy,
//      builder.loggingStrategy
//    )
//  }
//
//  // Whether the builder has something or not, used in type parameters.
//  abstract class HAS_SOME
//  abstract class HAS_NONE
//}
