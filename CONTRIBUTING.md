# Contributing to Evvo
First of all, thanks for looking at this page and considering contributing! We'd love to have you, and if you have any questions or this document is unclear, please let us know! Open an issue with your question, or email any contributor or admin at the emails found in our github profiles.

----------------------------------------------------------------------------------------------------
### Setting up
#### IntelliJ
We all use IntelliJ as our primary editor for this project. If you prefer to use a different editor, let us know - there are probably things we failed to describe in this file because IntelliJ set them up for us. To set up IntelliJ for this project, you can simply clone it from Github using the "Import from VCS" dialogue, or clone it from the command line and then use the "Open project" dialogue.


----------------------------------------------------------------------------------------------------
### Git Workflow
We are using tagged releases on master to indicate milestones. There is no develop branch, so if you are used to git flow, things will be a little different. Every feature to be released starts on a branch named `feature/…`, is merged into `master` after a pull request, and then is considered "released" only once we have a tagged release after that commit.

#### Branch Names
* Branches that implement new features should be named `feature/<the-feature-name>`
* Branches that improve documentation should be named `doc/<what-docs-are-updated>`. Of course, feature branches will include lots of documentation as well - this name is only for branches that exist solely to update documentation.
* Branches that fix bugs on a tagged release should be named `hotfix/<what's-being-fixed>`

#### Commit Messages
Commit messages should start with a brief description of what the commit does - a verb phrase, like "add CONTRIBUTING.md" or "test foo under bar conditions". This verb phrase should indicate what changes will be caused by applying the code to the codebase. Then, if there is more to your commit than what you could say in the header, list everything else in bullets after two newline. For example:

```
apply scalastyle to test

 * fix tests to comply with scalastyle
 * drop magic number checker
 * removed MatrixCluster
```

You don't need to describe the full detail of every change – we can read the code for that. Just a brief overview of each task the commit accomplishes.

#### Pull Requests
Pull requests require approval from an approved reviewer before being merged into master. Other branches do not have this protection, so merging together branches you have created locally won't cause any issues, and you can rebase your branch onto master as master is updated. Every pull request (and every commit run on a branch with an open pull request) will cause Travis build your code and run the whole test suite. Pull requests will only be allowed to merge if they pass the style checker and all tests. Pull request messages should be similar to commit messages.

#### ScalaStyle
We use [`scalastyle`](http://www.scalastyle.org) as our style checker. You can find the configuration at [project/scalastyle_config.xml](project/scalastyle_config.xml). This style configuration is liable to change, and we are open to suggestions – but make sure that any code you write passes the style configuration you have submitted. You can manually run the style checker with `mvn scalastyle:check`, which will print the filename and line number of everyu violation. You can also set up a pre-commit hook to run the style checker, as described [in the scalastyle docs](http://www.scalastyle.org/git-pre-commit-hook.html). Scalastyle is run on every pull request.

#### Issues, Projects, Milestones
Development is guided entirely by our [issues page](https://github.com/evvo-labs/evvo/issues). We use milestones to reflect which issues must be closed before the next tagged release. So, if you are looking for issues to start working on, start with issues tagged with the next milestone – those are the most pressing. Projects are used to bundle issues that all touch the same component, or which depend on each other. In other words, if having a kanban board would be useful, use a project so that you get a kanban board.

----------------------------------------------------------------------------------------------------
Now that you understand how to develop on this project, you may want a better understanding of the project internals so that you can start contributing! For that, [`doc/ARCHITECTRURE.md`](doc/ARCHITECTURE.md) will help you get started. Examples of using the framework can be found in the [examples](examples) directory. If you even read this far, we'd love feedback on how to make this file better – go ahead, file your first issue telling us what could be more clear!
