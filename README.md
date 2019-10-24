# Java Embedded Keycloak library 
Original idea comes from [thomasdarimont](https://github.com/thomasdarimont)

## Core Usage
You have to run the EmbeddedKeycloakApplication class in a resteasy's servlet context
(this is Keycloak's limitation)

## Configuration
Embedded keycloak server can be configured in some ways. There are few limitations that cannot be easily overcome.

Configuration consists of 3 files:
- keycloak.properties
- keycloak-realm-conf.json
- keycloak-server.conf

Above configuration files may be overriden by adding your versions to classpath. Description below.

## Custom configuration
keep in mind that changing this file might not affect your Keycloak's configuration
in order to do so, please remove Keycloak's embedded database files

### Custom keycloak-server.conf
I exported this file from standalone Keycloak. I wouldn't recommend touching it unless you know what are you doing since you will modify Keycloak's intenstinals ;)

### Custom keycloak-realm-conf.json
Please refer to Keycloak's documentation for more details, however, this file is quite intuitive for reading and understanding.

### Custom keycloak.properties configuration
This file could be used to configure service context, admin user, default realm etc.
 
### Setting Embedded Keycloak's context path
`keycloak.embedded.server.context-path=/auth`

### Setting Keycloak server basic configuration file
`keycloak.embedded.server.configuration.path=keycloak-server.conf`

### Setting default realm
`keycloak.embedded.realm.default.name=master`

### Setting Keycloak's realm configuration file
 `keycloak.embedded.realm.configuration.path=keycloak-realm-conf.json`

### Setting behaviour when Keycloak during startup finds existing configuration
`keycloak.embedded.realm.configuration.strategy=OVERWRITE_EXISTING`

available strategies = `[IGNORE_EXISTING, OVERWRITE_EXISTING]` from `org.keycloak.exportimport.Strategy`

### Settings Keycloak's default admin user credentials
`
keycloak.embedded.security.admin.username=admin
keycloak.embedded.security.admin.password=admin
`

### Setting Keycloak's datasource
`
keycloak.embedded.datasource.url=jdbc:h2:./data/keycloak;DB_CLOSE_ON_EXIT=FALSE
keycloak.embedded.datasource.username=sa
keycloak.embedded.datasource.password=
`

## Spring integration

### Usage
Just annotate one of your configuration classes with: `@EnableEmbeddedKeycloakAutoConfiguration` and that's it!
If you are still confused check out example project in this repository: `embedded-keycloak-spring-example`

### Use it!
Currently it's not publicly available in maven central repository, however - stay tuned!

Gradle
```
compile group: 'pl.grizzlysoftware', name: 'embedded-keycloak-spring', version: '1.0.0'
```
```
compile 'pl.grizzlysoftware:embedded-keycloak-spring:1.0.0'
```
Maven

```
<dependency>
    <groupId>pl.grizzlysoftware</groupId>
    <artifactId>embedded-keycloak-spring</artifactId>
    <version>1.0.0</version>
</dependency>
```


## Troubleshooting
I haven't found any troubles in running it with spring tests except one(as for today - 14.11.2019).
Keycloak resolves datasource via JNDI so in order to provide our own datasource we have to mock it.
The thing is that attempt to mock it second time causes error. I haven't got time to investigate it. However
simple check whether the JNDI context factory does not solve the problem - Keycloak does not work as expected.
Above situation may be caused when Spring Configuration is modified during tests execution - i.e. by overriding some beans. Then 
spring instantiates configuration again because it doesn't match one that was instantiated before.