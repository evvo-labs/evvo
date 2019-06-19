package com.evvo.island

import akka.actor.{ActorSystem, Props}
import com.evvo.agent.{CreatorFunction, DeletorFunction, ModifierFunction, MutatorFunction}
import com.evvo.island.EvvoIslandBuilder.HAS_SOME
import com.evvo.island.population.Objective

/**
  * <p>
  * A builder for `Local` and `RemoteEvvoIsland`s. See
  * [[http://blog.rafaelferreira.net/2008/07/type-safe-builder-pattern-in-scala.html]]
  * for an explanation of the type parameters. Users should never interact with this class directly,
  * instead getting a result from `EvvoIsland.builder()` or `EvvoIslandBuilder()`, adding the
  * required data, and then implicitly converting to a `FinishedEvvoIslandBuilder`.
  * </p>
  *
  * <p>
  * Can be implicitly converted to a [[com.evvo.island.FinishedEvvoIslandBuilder]] once it
  * has had at least one creator, one mutator, one deletor, and one objective added.
  * </p>
  *
  * @param creators   the functions to be used for creating new solutions.
  * @param modifiers   the functions to be used for creating new solutions from current solutions.
  * @param deletors   the functions to be used for deciding which solutions to delete.
  * @param objectives the objective functions to maximize.
  * @tparam HasCreators   HAS_SOME if the builder has at least one creator, HAS_NONE otherwise.
  * @tparam HasModifiers   same as HasCreators
  * @tparam HasDeletors   same as HasCreators
  * @tparam HasObjectives same as HasCreators
  */
case class UnfinishedEvvoIslandBuilder[Sol, HasCreators, HasModifiers, HasDeletors, HasObjectives]
(
  creators: Set[CreatorFunction[Sol]] = Set[CreatorFunction[Sol]](),
  modifiers: Set[ModifierFunction[Sol]] = Set[ModifierFunction[Sol]](),
  deletors: Set[DeletorFunction[Sol]] = Set[DeletorFunction[Sol]](),
  objectives: Set[Objective[Sol]] = Set[Objective[Sol]]()
) {
  def addCreator(creatorFunc: CreatorFunction[Sol]):
  UnfinishedEvvoIslandBuilder[Sol, HAS_SOME, HasModifiers, HasDeletors, HasObjectives] = {
    UnfinishedEvvoIslandBuilder(creators + creatorFunc, modifiers, deletors, objectives)
  }

  def addModifier(modifierFunc: ModifierFunction[Sol]):
  UnfinishedEvvoIslandBuilder[Sol, HasCreators, HAS_SOME, HasDeletors, HasObjectives] = {
    UnfinishedEvvoIslandBuilder(creators, modifiers + modifierFunc, deletors, objectives)
  }

  def addDeletor(deletorFunc: DeletorFunction[Sol]):
  UnfinishedEvvoIslandBuilder[Sol, HasCreators, HasModifiers, HAS_SOME, HasObjectives] = {
    UnfinishedEvvoIslandBuilder(creators, modifiers, deletors + deletorFunc, objectives)
  }

  def addObjective(objective: Objective[Sol]):
  UnfinishedEvvoIslandBuilder[Sol, HasCreators, HasModifiers, HasDeletors, HAS_SOME] = {
    UnfinishedEvvoIslandBuilder(creators, modifiers, deletors, objectives + objective)
  }
}

/**
  * A finished EvvoIslandBuilder can build an actual island. See
  * [[com.evvo.island.UnfinishedEvvoIslandBuilder]] for more information.
  */
case class FinishedEvvoIslandBuilder[Sol]
(
  creators: Set[CreatorFunction[Sol]],
  modifiers: Set[ModifierFunction[Sol]],
  deletors: Set[DeletorFunction[Sol]],
  objectives: Set[Objective[Sol]]
) {
  assert(creators.nonEmpty)
  assert(modifiers.nonEmpty)
  assert(deletors.nonEmpty)
  assert(objectives.nonEmpty)

  def toProps()(implicit system: ActorSystem): Props = {
    Props(new RemoteEvvoIsland[Sol](
      creators.toVector,
      modifiers.toVector,
      deletors.toVector,
      objectives.toVector))
  }

  def buildLocalEvvo(): EvolutionaryProcess[Sol] = {
    new LocalEvvoIsland[Sol](
      creators.toVector,
      modifiers.toVector,
      deletors.toVector,
      objectives.toVector)
  }
}

object EvvoIslandBuilder {
  /**
    * @return a new, unfinished EvvoIslandBuilder.
    */
  def apply[Sol](): UnfinishedEvvoIslandBuilder[Sol, HAS_NONE, HAS_NONE, HAS_NONE, HAS_NONE] = {
    UnfinishedEvvoIslandBuilder()
  }

  /**
    * Implicitly converts a builder with all required fields into a Finished builder.
    * @param builder the builder to convert
    * @return A FinishedEvvoIslandBuilder with the same data as `builder`
    */
  implicit def finishBuilder[Sol]
  (
    builder: UnfinishedEvvoIslandBuilder[Sol, HAS_SOME, HAS_SOME, HAS_SOME, HAS_SOME]
  ): FinishedEvvoIslandBuilder[Sol] = {
    FinishedEvvoIslandBuilder(
      builder.creators, builder.modifiers, builder.deletors, builder.objectives)
  }


  // Whether the builder has something or not, used in type parameters.
  abstract class HAS_SOME
  abstract class HAS_NONE
}