## Modules

The main parts of the project are:

* all: a single content package that embeds all the compiled modules (bundles and content packages)
* core: Java bundle containing all core functionality like RestrictionPattern
* examples: this module contains content packages containing predefined users, groups, configs
* examples.ui.apps: contains the /apps parts of the examples
* examples.ui.content: contains sample content using the components from the examples.ui.apps
* examples.ui.config: contains runmode specific OSGi configs for the examples



## How to build/deploy

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the following command:

    mvn clean install -PautoInstallSinglePackage

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallSinglePackagePublish

Or alternatively

    mvn clean install -PautoInstallSinglePackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

Or to deploy only a single content package, run in the sub-module directory (i.e `examples.ui.apps`)

    mvn clean install -PautoInstallPackage


## SonarQube
Sonarqube results can be seen on https://sonarcloud.io/project/overview?id=aapm.

### Unit tests

To test, execute:

    mvn clean test
