# Contributing to Evvo

### Akka Architecture Diagram
![Akka Architecture Overview](./Island%20Cluster%20Architecture.png)

-------------------------------------------------------------------------------
### Modules
#### [`agent`](./src/main/scala/com/evvo/agent/)
This holds the agents (classes that extend `TAgent`), and supporting classes such as strategies. Each agent extends an abstract class ('`AAgent`'), which  implements common features of agents: logging, using a separate thread for work, and having that thread wait the amount of time that the strategies say to. New agent classes should extend 

#### [`island`](./src/main/scala/com/evvo/island/)
Implements islands and clusters of islands. The main point of this package are the classes that extend `TEvolutionaryProcess`. These classes are ready-to-use implementations of evolutionary processes, that only need to have creators, fitness functions, etc. plugged in to start solving problems. Each island has its own population.

#### [`island/population`](src/main/scala/com/evvo/island/population/)
Holds `TPopulation`, `Population`, and the classes required to support them. The population is a set of scored ('`TScored`') solutions, where uniqueness and equality are (by default) measured by the score and not the solution itself. This is for speed (we can hash a smaller piece of data - the score mapping), and for speed (because we have fewer solutions in the population, and anything with duplicate scores should be just as good as a solution).


-------------------------------------------------------------------------------
##### Constructing an Island/Cluster
We use Akka for network-parallelism between [islands](../README.md#terminology). Islands are created with an `IslandBuilder`, a class that specifies what happens in an island – mutators, deletors, and so on. An instance of this class is passed to an `IslandManager`, which creates some number of the specified type of island. 

