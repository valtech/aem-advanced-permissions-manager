## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality like OSGi service provider, RestrictionPattern, as well as component-related Java code such as servlets or request filters.
* it.tests: Java based integration tests
* ui.apps: contains the /apps (and /etc) parts of the project, ie JS&CSS clientlibs, components, and templates
* ui.content: contains sample content using the components from the ui.apps
* ui.config: contains runmode specific OSGi configs for the project
* ui.frontend: an optional dedicated front-end build mechanism (Angular, React or general Webpack project)
* ui.tests: Selenium based UI tests
* all: a single content package that embeds all the compiled modules (bundles and content packages) including any vendor dependencies
* analyse: this module runs analysis on the project which provides additional validation for deploying into AEMaaCS
* examples: this module is used to build content packages (without the code) containing predefined users, groups, configs "ready to play"


## How to build/deploy

#### WARNING : You would maybe need to deploy twice (and even 3 times) the project because some folders and files are not well deployed with Actools


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

Or to deploy only a single content package, run in the sub-module directory (i.e `ui.apps`)

    mvn clean install -PautoInstallPackage

## How to create a release without user/group and content packages

    mvn clean package -PwithoutPreinstalledExamples -DskipTests

## How to create an "example package" with only preinstalled examples of users/groups and content packages (does not include the release)
    
    mvn clean package -PonlyPreinstalledExamples -DskipTests (maybe needed to install it twice or 3 times, there are sometimes some problems during actools installation)


## SonarQube
- You need to install a SonarQube server
    - for example you can use : ```https://github.com/ahmed-musallam/sonarqube-aem```

- Then go to ```core``` project

- Launch : ```mvn clean install; mvn sonar:sonar```

- Reporting files are thus generated under ```/core/target/site/jacoco-aggregate/jacoco.xml```

- SonarQube is installed by default on port 9000  (```http://localhost:9000```)
    - to get another port set ```sonar.host.url```

### Unit tests

This show-cases classic unit testing of the code contained in the bundle. To
test, execute:

    mvn clean test