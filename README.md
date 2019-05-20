# Evvo by Diatom Labs ![travis master status](https://travis-ci.org/evvo-labs/evvo.svg?branch=master) 


Evvo is the Scala framework for [multi-objective](https://en.wikipedia.org/wiki/Multi-objective_optimization) [evolutionary computing](https://en.wikipedia.org/wiki/Evolutionary_computation). The primary design goals are providing the best possible interface for developers, network parallelism, first-class support for any type of problem, and extensible configurations with sane defaults.

Here's an example showing how simple it is to set up and solve a basic problem (on one machine) using Evvo:
```scala
import com.diatom.island._ 
import scala.concurrent.duration._ // for `1.second`

val islandBuilder = EvvoIsland.builder[Solution]()
  .addCreator(creator)
  .addMutator(mutator)
  .addDeletor(deletor)
  .addObjective(objective1)
  .addObjective(objective2)

// create five "islands", run each one for 1 second, in parallel
val islandManager = new IslandManager[Solution](5, islandBuilder)
  .run(TerminationCriteria(1.second))

// collects the pareto frontier from each island, and returns the non-dominated set from there 
val paretoFrontierAfter1Second = islandManager.currentParetoFrontier()
```

This will run three asynchronous agents locally: _creators_ create new solutions, _mutators_ copy and modify existing solutions, and _deletors_ remove solutions from the island. Notice how this whole block of code will work on any problem, as long as each of the functions can process that type of data. After spinning for a second, the blocking call to `run(…)` will return. The current [pareto frontier](https://en.wikipedia.org/wiki/Pareto_efficiency#Use_in_engineering) is then retrieved and bound to `paretoFrontierAfter1Second`. This result will be a diverse set of solutions optimizing for each provided objective. 

This example assumes that you have a defined `Solution` type, a creator function, a mutator function, and a deletor function, as well as two fitness functions. While we can provide a framework for solving evolutionary computing problems in parallel, it is impossible to provide functions that will work for any problem. (There are some exceptions, which you can read more about in _"Built-in Agents"_ and _"Built-in Problem Types"_.) However, writing these functions will be problem-specific, so we leave that to end users and application developers.

-------------------------------------------------------------------------------
#### Terminology
**Actor**: an Akka actor.

**Agent**: _not_ an Akka actor, but a part of an asynchronous evolutionary system. See _"Asynchronous Evolutionary Computing"_ for further explanation.

**Agent Strategy**: a function that consumes information about the state of the world and tells an agent how often it should be running.

**Creator Agent**: often shortened to "Creator", a Creator Agent calls a function to get a set of solutions, and then adds those solutions to the population. 

**Deletor Agent**: often shortened to "Deletor", a Deletor Agent retrieves some number of solutions from the population, calls a function on that set of solutions, and deletes whichever solutions the function says to delete.

**Diversity**: a measure of how similar the solutions in a population are. The exact measure can be customized for each problem, and then computed based on  some metric of solution's distances, or computed based on the scores according to fitness functions, in which case standard measures of distance in Euclidean space can be used.

**Dominated Solution**: a dominated solution is worse in every regard than another solution, or worse in one regard and equal on others. For example, if there are three objective functions, each of which is to be maximized, then a solution scoring `(3, 9, 4)` would be dominated by one scoring `(5, 11, 4)`, but not one scoring `(5, 11, 3)`.

**Emigration**: an island sending solutions to another island.

**Immigration**: an island receiving new solutions from another island

**Island**: the second-largest unit of the evolutionary computing system: a population, shared among multiple actors modifying the population in various ways, and a network component that handles immigration and emigration. 

**Island System** the entire evolutionary computing system: consisting of multiple islands, a manager of those islands, and interactions between the islands.

**Mutator Agent**: often shortened to "Mutator", a Mutator Agent retrieves some number of solutions from the population, calls a function on that set of solutions to produce new solutions based on the input, and adds those new solutions to the population.

**Pareto Frontier**: a set of [non-dominated solutions](https://en.wikipedia.org/wiki/Pareto_efficiency#Pareto_frontier).

**Popluation**: each island has a population, which is simply a set of scored solutions. Agents interact with the population, and the goal of the whole system is to make the pareto frontier of the population as high-quality as possible.

-------------------------------------------------------------------------------
#### Asynchronous Evolutionary Computing
As described in [John Rachlin's paper on paper mill optimization](https://www.researchgate.net/profile/Richard_Goodwin2/publication/245797473_Cooperative_Multiobjective_Decision_Support_for_the_Paper_Industry/links/0046352ca1becd5890000000.pdf), asynchronous multi-agent evolutionary computing systems consist of a common population and multiple "evolutionary agents" that operate on the population. The three basic types of agents are creators, mutators, and deletors: creators make new solutions to add to the population, mutators read some solutions and use them as a basis for new solutions, and deletors read some solutions and decide which should be deleted. These agents, working in parallel, gradually push the overall fitness of a population upwards. (Assuming, of course, that the mutators have a chance of improving fitness, and the deletors remove solutions that tend to be worse than average.) This system is easily parallelizable, as there is only one piece of shared memory - the set of solutions currently in the population. Much of the work (the work of computing new solutions, mutating existing solutions, and deciding which solutions to delete) can be distributed across multiple CPU cores, or even multiple machines.

-------------------------------------------------------------------------------
#### Quickstart
##### Built-in Agents
While each type of problem needs a separate type of creator and mutator, deletors can be shared between problems.
We provide default deletor agents in [defaults.scala](src/main/scala/com/diatom/agent/defaults/defaults.scala). One example is the `DeleteDominated` agent. This agent takes a sample from the population, and deletes any solutions in the sample that were dominated by any other solution in the sample. These built-in deletors can be used in one line of code, for example, to add a deletor that will take samples of size 32 and delete the dominated set.

```scala
EvvoIsland.builder[Solution]()
  // ... add creators, mutators, objective functions
  .addDeletor(DeleteDominated(numInputs=32))
```


##### Built-in Problem Types
TODO: Implement built-in types (bitstrings, trees, vector of floats, 0-1 knapsack on bitstring, TSP)

-------------------------------------------------------------------------------
#### Configuration
##### Akka Configuration
TODO: Allow end users to override Akka configuration
##### Evvo Configuration
TODO: Allow end users to override Evvo configuration

-------------------------------------------------------------------------------
#### Examples
See examples in [`./examples`](examples). These examples show how you can read data, create solutions types, and then write creators, mutators, and deletors to find good solutions to optimization problems.

-------------------------------------------------------------------------------
#### Downloads
TODO: Put this on maven and sbt

-------------------------------------------------------------------------------
#### Contributing
See [`CONTRIBUTING.MD`](CONTRIBUTING.md) for information on how to contribute code to Evvo.

-------------------------------------------------------------------------------
#### 
See [`ARCHITECTURE.md`](doc/ARCHITECTURE.md).
