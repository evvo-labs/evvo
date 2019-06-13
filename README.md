# Evvo ![travis master status](https://travis-ci.org/evvo-labs/evvo.svg?branch=master) 


Evvo is the Scala framework for distributed [multi-objective](https://en.wikipedia.org/wiki/Multi-objective_optimization) [evolutionary computing](https://en.wikipedia.org/wiki/Evolutionary_computation).
 
 Here's an example showing how simple it is to set up and solve a basic problem (on one machine) using Evvo. Let's say we want to maximize the number of ones in a `Bitstring`. To represent this objective, we'll need to write an `Objective` (who would have guessed?). Then we can use Evvo to search for `Bitstring`s that satisfy the `Objective`. After running for a second, we'll print the current [pareto frontier](https://en.wikipedia.org/wiki/Pareto_efficiency#Use_in_engineering). 
```scala
import com.evvo.agent.defaults.{Bitstring, Bitflipper, BitstringGenerator, DeleteDominated}
import com.evvo.island.{EvvoIsland, LocalIslandManager, StopAfter}
import com.evvo.island.population.{Maximize, Objective}
import scala.concurrent.duration._

object Maximize1Bits extends Objective[Bitstring]("1Bits", Maximize) {
  override protected def objective(sol: Bitstring): Double = {
    sol.count(identity) // Bitstrings are represented as Seq[Boolean]
  }
}

val islandBuilder = EvvoIsland.builder[Bitstring]()
  .addCreator(BitstringGenerator(length=16))
  .addMutator(Bitflipper())
  .addDeletor(DeleteDominated[Bitstring]())
  .addObjective(Maximize1Bits)

val islandManager = new LocalIslandManager(numIslands = 1, islandBuilder)
islandManager.runBlocking(StopAfter(1.second))

print(islandManager.currentParetoFrontier())
```

This will run three asynchronous agents locally. `BitstringGenerator` creates `Bitstring`s of length 16 with randomly initialized bits, `Bitflipper` flips a random bit, and `DeleteDominated` will pick `Bitstring`s out of the generated solutions and delete the [dominated](https://en.wikipedia.org/wiki/Pareto_efficiency#Use_in_engineering_and_economics) ones. If you run this code on anything more powerful than a toaster, here's what will be printed:
```
ParetoFrontier(Map(1Bits -> 16.0))
```

This means that there was one solution on the pareto frontier, which scored `16.0` according to the objective named `"1Bits"`. Since the generated `Bitstring`s only have 16 bits, the best possible score is `16.0` bits that are 1.

If our [built-in](./src/main/scala/com/evvo/agent/defaults/defaults.scala) _Creators_, _Mutators_, and _Deletors_ do not work for your problem, you can define your own as easily as we defined `Maximize1Bits`.

-------------------------------------------------------------------------------
### Terminology

**Island**: a _population_ with multiple _agents_ which can communicate with other islands for _immigration_ and _emigration_.

**Population**: a mutable set of scored solutions from which the _Pareto Frontier_ is calculated.

**Pareto Frontier**: a set of [non-dominated solutions](https://en.wikipedia.org/wiki/Pareto_efficiency#Pareto_frontier).

**Dominated Solution**: a dominated solution is worse in every regard (as captured by the _objectives_) than another solution, or worse in one regard and equal on others. For example, if there are three objective functions, each of which is to be maximized, then a solution scoring `(3, 9, 4)` would be dominated by one scoring `(5, 11, 4)`, but not one scoring `(5, 11, 3)`.

**Objective**: a single goal in the broader optimization problem.

**Fitness**: how strong a solution is as determined by the _objectives_.

**Agent**: _not_ an Akka actor, but a part of an [asynchronous evolutionary system](#asynchronous-evolutionary-computing). Each one runs on a separate thread.

**Creator Agent**: often shortened to "Creator", a Creator Agent generates a set of solutions and adds those solutions to the _population_. 

**Mutator Agent**: often shortened to "Mutator", a Mutator Agent retrieves some number of solutions from the _population_, calls a function on that set of solutions to produce new solutions based on the input, and adds those new solutions to the _population_.

**Deletor Agent**: often shortened to "Deletor", a Deletor Agent retrieves some number of solutions from the _population_ and deletes the bad ones (for whatever definition of bad it's working with) from the _population_.

**Emigration**: an _island_ sending solutions to another _island_.

**Immigration**: an _island_ receiving solutions from another _island_.

-------------------------------------------------------------------------------
### The Model
#### Asycnchronous Evolutionary Computing
As described in [John Rachlin's paper on paper mill optimization](https://www.researchgate.net/profile/Richard_Goodwin2/publication/245797473_Cooperative_Multiobjective_Decision_Support_for_the_Paper_Industry/links/0046352ca1becd5890000000.pdf), asynchronous multi-agent evolutionary computing systems consist of a common population and multiple "evolutionary agents" that operate on the population. These agents, working in parallel, gradually push the overall fitness of a population upwards. (Assuming that the mutators have a chance of improving fitness, and the deletors remove solutions that tend to be worse than average.) This system is easily parallelizable, as there is only one piece of shared memory - the set of solutions currently in the population. Much of the work (the work of computing new solutions, mutating existing solutions, and deciding which solutions to delete) can be distributed across multiple CPU cores, or even multiple machines.

This diagram illustrates each of the major components in Evvo and their roles:
```
+---------------------------------------------------------------------------+
|                                                                           |
| EvvoIsland                                                                |
|                                                                           |
+---------------------------------------------------------------------------+
|                                                                           |
|                                                   +-------------------+   |
|  +---------------+       Generates new solutions  |                   |   |
|  |               |<-------------------------------| Creator Agent(s)  |   |
|  | Population    |                                |                   |   |
|  |               |                                +-------------------+   |
|  +---------------+                                                        |
|  |               | Reads some solutions           +-------------------+   |<----+
|  |  - Objectives |------------------------------->|                   |   |     |
|  |               |                                | Mutator Agent(s)  |   |     |
|  |  - Solutions  |<-------------------------------|                   |   |     |  Immigration / Emigration
|  |               |         Derives new solutions  +-------------------+   |     |
|  |               |                                                        |     |    Peer-to-peer gossip
|  |               | Reads some solutions           +-------------------+   |     |    protocol for sharing
|  |               |------------------------------->|                   |   |     |    solutions increases
|  |               |                                | Deletor Agent(s)  |   |     |    scalability without
|  |               |<-------------------------------|                   |   |     |    converging to local
|  |               |        Chooses some to delete  +-------------------+   |     |    optima.
|  +---------------+                                                        |     |
|                                                                           |     |    This is how Evvo takes
+---------------------------------------------------------------------------+     |    advantage of parallelism.
                                   ^                                              |
                                   |  Immigration / Emigration                    |
                                   v                                              |
+---------------------------------------------------------------------------+     |
|                                                                           |     |
| EvvoIsland     [Same contents as above, abbreviated for clarity]          |     |
|                                                                           |     |
+---------------------------------------------------------------------------+     |
                                   ^                                              |
                                   |  Immigration / Emigration                    |
                                   v                                              |
+---------------------------------------------------------------------------+     |
|                                                                           |     |
| EvvoIsland     [Same contents as above, abbreviated for clarity]          |<----+
|                                                                           |
+---------------------------------------------------------------------------+

                                    .
                                    .
                                    .

                                and so on
```


### Distributing islands on a network
This diagram shows a simplified version of our network model:
```
                                                         +------------------------------------------------+
                                                         |                                                |
                                                         | Server                                         |
                                                         |                                                |
                                                         | +--------------------------------------------+ |
                                                         | |                                            | |
                                                  +----->| | JVM running ActorSystem with remoting      | |
                                                  |      | |                                            | |
                                                  |      | |  +----------+  +----------+  +----------+  | |
+---------------------------------+               |      | |  |          |  |          |  |          |  | |
|                                 |               |      | |  | Island 1 |  | Island 4 |  | Island 7 |  | |
| IslandManager (client)          |               |      | |  |          |  |          |  |          |  | |
|                                 |               |      | |  +----------+  +----------+  +----------+  | |
+---------------------------------+               |      | |                                            | |
|                                 |               |      | +--------------------------------------------+ |
|  - Round robin deploys Islands  |<--------------+      |                                                |
|     to remote ActorSystems      |               |      +------------------------------------------------+
|                                 |               |             ^
|  - Combines results at end      |               |             |  Some gossip happens over network
|     of optimization             |               |             |  Some gossip happens within a server
|                                 |               |             v
+---------------------------------+               |      +------------------------------------------------+
                                                  |      |                                                |
                                                  |      | Server                                         |
                                                  |      |                                                |
                                                  |      | +--------------------------------------------+ |
                                                  |      | |                                            | |
                                                  +----->| | JVM running ActorSystem with remoting      | |
                                                  |      | |                                            | |
                                                  |      | |  +----------+  +----------+  +----------+  | |
                                                  |      | |  |          |  |          |  |          |  | |
                                                  |      | |  | Island 2 |  | Island 5 |  | Island 8 |  | |
                                                  |      | |  |          |  |          |  |          |  | |
                                                  |      | |  +----------+  +----------+  +----------+  | |
                                                  |      | |                                            | |
                                                  |      | +--------------------------------------------+ |
                                                  |      |                                                |
                                                  |      +------+-----------------------------------------+
                                                  |             ^
                                                  |             |
                                                  |             v
                                                  |      +------+-----------------------------------------+
                                                  |      |                                                |
                                                  |      | Server                                         |
                                                  |      |                                                |
                                                  |      | +--------------------------------------------+ |
                                                  |      | |                                            | |
                                                  +----->| | JVM running ActorSystem with remoting      | |
                                                         | |                                            | |
                                                         | |  +----------+  +----------+                | |
                                                         | |  |          |  |          |                | |
                                                         | |  | Island 3 |  | Island 6 |                | |
                                                         | |  |          |  |          |                | |
                                                         | |  +----------+  +----------+                | |
                                                         | |                                            | |
                                                         | +--------------------------------------------+ |
                                                         |                                                |
                                                         +------------------------------------------------+

                                                                                 .
                                                                                 .
                                                                                 .

                                                            and so on, until your server budget runs out
```

#### Setting up Servers
Evvo is [dockerized](https://www.docker.com/). Follow the [instructions](docker/README.md) to get started running your own network parallel instance.

#### Serializability

-------------------------------------------------------------------------------
### Configuration
### Akka Configuration
TODO: Allow end users to override Akka configuration

-------------------------------------------------------------------------------
### Downloads
We plan to release Evvo so that it can be with Maven or sbt. The project is not yet stable enough for that to make sense.

-------------------------------------------------------------------------------
### Contributing
See [`CONTRIBUTING.MD`](CONTRIBUTING.md) for information on how to contribute code to Evvo.
