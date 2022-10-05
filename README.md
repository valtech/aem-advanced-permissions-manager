# AEM Advanced Permissions Manager

AEM Advanced Permissions Manager uses custom Oak restriction mechanism to provide a way to apply the permission.
The permission is applied only if the defined condition is met. Currently the main example is to allow/deny access
if an asset owns (or does not own) a tag.

This mechanism can be used to restrict a standard ACL permission for any asset.

# Requirements

AAPM requires Java 11, Maven >= 3.6.0 and AEM Cloud/6.5.


| AEM Version | AAPM        |
|-------------|-------------|
| Cloud       | 0.9.x       |
| 6.5         | 0.9.x       |

# Deploying
There are 2 ways to deploy:
- From zip files
  - Code: aapm.all-x.x.x.jar
  - Examples including predefined assets, users and groups to test quickly: aapm.examples-x.x.x.zip
- From sources 
  - See our [developer zone](docs/developers.md)

    
### Deploy as Maven dependency:
    <dependency>
        <groupId>com.valtech.aapm</groupId>
        <artifactId>aapm.all</artifactId>
        <version>LATEST</version>
        <type>zip</type>
    </dependency>


## Uninstallation

### Removing following paths
- /conf/valtech/aapm-examples
- /content/dam/aapm-test
- /apps/valtech/aapm
- /apps/valtech/aapm-examples

### Deleting users and groups (in /security/users.html)
#### Users

- utest-aapm-content
- utest-aapm-reader
- utest-aapm-restricted


#### Delete following groups

- aapm-content
- aapm-reader
- aapm-restricted

## Define restriction 
User with the appropriate rights, can define restrictions.

### Build a restriction

A restriction is written like this:

- rep:hasPropertyValues: <restriction_type>#<unary_operator><property_type>$<property_name><binary_operator><property_value>

where:
- <restriction_type> = "allow" or "deny"
- <unary_operator> = "!" or "" (negation or not)
- <property_type> = "int", "date", "string" (*currently only type "string" works well*)
- <property_name> = the name of the asset node property ("cq:tags" for example)
- <binary_operator> = 
    - "*_EQUALS*_" 
    - "*_GREATER_THAN_EQUALS*_"
    - "*_LESS_THAN_EQUALS*_" 
    - "*_GREATER_THEN*_" 
    - "*_LESS_THEN*_" (*currently only "*_EQUALS*_" works well*)
- <property_value> = the value the property has to be equal to match the restriction

#### Examples
You can install the aapm.examples package for the following examples.
*Be careful: the permission type (allow or deny) has to be the same than the "restriction type". See examples below.*

##### Example 1
- Permission type = "*deny*"
- hasPropertyValues: *deny*_string_cq:tags_EQUALS_properties:orientation/portrait
- For a restriction "R1" defined by the above line applying to a user "UserA", R1 will prevent UserA to access to all
  the assets tagged with orientation/portrait

##### Example 2
- Permission type = "*allow*"
- You can "cancel" the restriction of example 2 in a subfolder by adding following line
- hasPropertyValues: *allow*_string_cq:tags_EQUALS_properties:orientation/portrait

##### Example 3
- Permission type = "*deny*"
- You can use a "negate operator":
    - hasPropertyValues: *deny*_string_!cq:tags_EQUALS_properties:orientation/portrait
- For a restriction "R2" defined by the above line applying to a user "UserA", R2 will prevent UserA to access to all
the assets not tagged with orientation/portrait

### Restriction through permission tab
 1 - Go to permission tab (Tools/Security/Permission)
![from permission tab, user can modify or add new restriction](illustrations/aapm-Permission tab.png "Permission tab")
 2- Click "Add ACE" and follow instructions 
![user can add new restriction through this tab](illustrations/aapm-add-new-restriction.png "add ace")
### Restriction through repoinit file
  config file location: apps/aapm-examples/osgiconfig/config/org.apache.sling.jcr.repoinit.RepositoryInitializer-aapm.config

    # Test 1: with user that has permission to see the all inside test-allow folder
    - aapm-default-reader:
    - path: /content/dam/aapm-test/test-allow
      permission: allow
      actions:
      privileges: jcr:all
      restrictions:
        hasPropertyValues: allow#string$cq:tags==properties:orientation/portrait

    # Test 2: with user that has NO permission on test-deny but with permission in test-deny/subfolder
    - path: /content/dam/aapm-test/test-deny
      permission: deny
      actions:
      privileges: jcr:all
      restrictions:
        hasPropertyValues: deny#string$cq:tags==properties:orientation/portrait

    - path: /content/dam/aapm-test/test-deny/subfolder
      permission: allow
      actions:
      privileges: jcr:all
      restrictions:
        hasPropertyValues: allow#string$cq:tags==properties:orientation/portrait

## Modify Restriction

 User can modify existing restriction even through yaml or permission tab. 
 - From yaml, just modify your permission file and redeploy. 
 - From restriction tab, go to permission tab (Tools/Security/Permissions) and select the existing group and edit
![edit existing ACE](illustrations/aapm-see-and-modify-restriction.png "edit exisiting ace")

## Result according to yaml file definition

    # Test 1
    1 - Login as admin user
    2 - Navigate to /content/dam/aapm-test/test-allow folder
    4 - User should see all assets and sub folder
    3 - Unpersonnate as utest-aapm-reader (/apps/valtech/aapm-examples/aapm/permissions/users/aapm-ace-user.yaml)
    4 - Result: user will only see all assets with the tag properties:orientation/portrait
  ![Login with admin user account, user see all content](illustrations/aapm-admin-to-reader.png "Login with admin user account")
  ![on /test-allow folder, we apply restriction to apply allow permission only if asset has tag protrait](illustrations/aapm-reader.png "Impersonate as utest-aapm-reader")

    # Test 2
    1 - Login as admin user
    2 - Navigate to /content/dam/aapm-test/test-deny
    3 - User should see all assets and sub folder
    4 - Unpersonnate as utest-aapm-restricted (/apps/valtech/aapm-examples/aapm/permissions/users/aapm-ace-user.yaml)
    5 - Result: user should see only assets without the tag properties:orientation/portrait in "test-deny" and all asssets in "/subfolder" (because for the group "aapm-restricted" deny access for assets with tag "properties:orientation/portrait" has been overidden by an allow access for "subfolder")
  ![admin user see all content](illustrations/aapm-admin-to-restricted.png "Connect as admin user")
  ![admin user see all content](illustrations/aapm-assets-in-test-deny.png "Display utest-aapm-restricted assets for 'test-deny'")
  ![admin user see all content](illustrations/aapm-assets-in-subfolder.png "Display utest-aapm-restricted assets for 'subfolder")


# Developers

See our [developer zone](docs/developers.md).