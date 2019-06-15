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
    sol.count(identity) // Bitstrings are represented as Seq[Boolean], count `true`s
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

This means that there was one solution on the pareto frontier, which scored `16.0` according to the objective named `"1Bits"`. Since the generated `Bitstring`s only have 16 bits, the best possible score is `16.0` bits that are 1. Note that the pareto frontier doesn't print the actual solutions. The solutions are available within the `ParetoFrontier` class, but the `toString` method prints only the scores, because solutions to more complex problems are very large.

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

### Running a Custom Optimization Problem
This is going to change in the future, eventually you'll be able to include Evvo as a [Maven or sbt](#downloads) dependency. If you want to use Evvo before then, all of your code has to be part of the same src directory as Evvo (this is in order to ensure that remote islands can be [deserialized](#serializability)).

You'll want to check out our getting started (coming soon) for an example of how to use Evvo locally and remotely.

#### Keeping your Code Private
While we would love some well groomed examples in our repo, if you want to keep your code private, either put your new code in the [`src/main/scala/com/evvo/ignored`](src/main/scala/com/evvo/ignored) directory **or** locally ignore your new package/files with `git update-index --assume-unchanged [<file>...]`.

##### Best VCS Practices -- Advanced
Until we can be used as a dependency on Maven or sbt, things may look a little tricky. You can create a git repo inside the **ignored** directory, and use that to maintain your code.

You may also want to create a new repo containing 2 (`git submodules`)[https://git-scm.com/book/en/v2/Git-Tools-Submodules], Evvo and your optimization problem repository (still in the ignored directory).This way, you can update evvo and your custom project and ensure they stay in sync.

Here's how you can set it up:

```bash
mkdir my_new_project
cd my_new_project
echo "# README" > README.md

git submodule add git@github.com:evvo-labs/evvo.git
cd evvo/src/main/scala/com/evvo/ignored
git submodule add <YOUR_PROJECT>
cd ../../../../../../..
git submodule init

git add
git commit -m "Life finds a way"
```


#### Setting up Servers
Evvo is [dockerized](https://www.docker.com/). Follow the [instructions](docker/README.md) to get started running your own network-parallel instance.

#### Serializability
Because we have to ship Islands to remote servers, Islands need to be [serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html). All of the code provided by Evvo is serializable, but the creators, mutators, deletors, and objectives that you provide must also be serializable for the Islands to serialize and deserialize correctly. 

See [the relevant section](https://www.oreilly.com/library/view/scala-cookbook/9781449340292/ch12s08.html) of the Scala cookbook on serialization. Note that serialized classes should also have referential transparency, that is, they should not reference variables from an external scope. A class defined within an object (or another class, or a code block) that references variablees defined in the outer object (or class, or code block) may cause serialization issues if those values are not present in the deserialization context. In general, extending `Creator`, `Mutator`, or `DeletorFunction`, with case classes, and ensuring that those case classes take all the data they need as arguments will be sufficient to ensure that there are no serialization issues.

If you use a `LocalIslandManager` to create `LocalEvvoIsland`s, your data will still be serialized and deserialized, albeit on the same machine. This means that some of the most flagrant serialization exceptions can be caught early by testing with `LocalIslandManager`. 

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
