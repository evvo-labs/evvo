# Evvo by Diatom Labs

Evvo is the Scala framework for [multi-objective](https://en.wikipedia.org/wiki/Multi-objective_optimization) [evolutionary computing](https://en.wikipedia.org/wiki/Evolutionary_computation). The primary design goals are providing the best possible interface for developers, network parallelism, first-class support for any type of problem, and extensible configurations with sane defaults.

Here's an example showing how simple it is to set up and solve a basic problem (on one machine) using Evvo:

```scala
import com.diatom.island._
import scala.concurrent.duration._ // for 1.second

val paretoFrontierAfter1Second = 
     SingleIslandEvvo.builder[SolutionType]()
       .addCreator(creatorFunc)
       .addMutator(mutatorFunc)
       .addDeletor(deletorFunc)
       .addFitness(fitnessFunc1)
       .addFitness(fitnessFunc2)
       .build()
       .run(TerminationCriteria(1.second))
```

This will run three asynchronous agents locally: _creators_ create new solutions, _mutators_ copy and modify existing solutions, and _deletors_ remove solutions from the island. Notice how this whole block of code will work on any problem, as long as each of the functions can process that type of data. After spinning for a second, the blocking call to `run(…)` will return and the current [pareto frontier](https://en.wikipedia.org/wiki/Pareto_efficiency#Use_in_engineering) will be returned and bound to `paretoFrontierAfter1Second`. This result will be a diverse set of solutions optimizing for each provided objective. 

