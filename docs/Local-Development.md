# Local Development of Inji Certify

**Pre-requisites**: 

- Java 21, Postgres DB installed & configured
- Git Bash for Windows users

1. Clone the repo, usually the active development happens on the `develop` branch but one can check out a tagged version as well.
2. Run the DB init scripts present in `db_scripts/mosip_certify` , running `./deploy.sh deploy.properties` is a good way to init the DB.
3. Decide on the issuance mode of Certify. Some plugins enable Certify to operate as a Proxy and others enable it to work as an Issuer, configure `mosip.certify.plugin-mode` appropriately as `DataProvider` or `VCIssuance`.
    * [Recommended] Set it to `DataProvider` if you want a quickest possible working setup, and configure `mosip.certify.data-provider-plugin.issuer-uri` and `mosip.certify.data-provider-plugin.issuer-public-key-uri` appropriately.
    * If you have another Issuance module such as Sunbird, MOSIP Stack, you may want to set it up in `VCIssuance` mode.
4. Decide on the VCI plugin for use locally and configure it, while running locally from an IDE such as Eclipse or IntelliJ one needs to add configuration to the `application-local.properties` and add the VCI plugin dependency JAR to the certify-service project which implements one of `DataProviderPlugin` or `VCIssuancePlugin` interfaces.
5. Get a compatible eSignet setup running configured with the appropriate Authenticator plugin implementation matching the VCI plugin.
    * Configure `mosip.certify.authorization.url` to point to your Authorization service hostname, this could be a working eSignet instance or another AuthZ provider configured with an [Authenticator plugin implementation](https://docs.esignet.io/integration/authenticator), essentially enabling the VC Issuing plugin to do the work.
    * Configure `mosip.certify.domain.url`, `mosip.certify.identifier`, `mosip.certify.authn.issuer-uri`, `mosip.certify.authn.jwk-set-uri`, `mosip.certify.authn.allowed-audiences` appropriately as per the Authorization service and Certify URI.
    * Update the `mosip.certify.key-values` with the well known appropriately, with the correct credential-type, scope and other relevant attributes.
    * Update the well known configuration in `mosip.certify.key-values` to match the Credential type, scope and other fields to match your VerifiableCredential.
    * Appropriately configure the `mosip.certify.authn.allowed-audiences` to allowed audiences such that it matches with the AuthZ token when the Credential issue request is made to Certify.
6. (required if Mobile driving license configured) Onboard issuer key and certificate data into property `mosip.certify.mock.mdoc.issuer-key-cert` using the creation script.
7. Perform Authentication & VC Issuance to see if the Certify & AuthZ stack is working apprpriately. Look out for the Postman collections referred to in the main README.md of this project.

## Run certify locally with default setup:
1. 1. Clone the repo, usually the active development happens on the `develop` branch but one can check out a tagged version as well.
2. Run the DB init scripts present in `db_scripts/mosip_certify` , running `./deploy.sh deploy.properties` is a good way to init the DB.
3. The default setup uses the `MockCSVDataProviderPlugin` and the usecase is a farmer usecase where all the required configurations are already added to the [application-local.properties](../certify-service/src/main/resources/application-local.properties).
4. The runtime dependency for the CSV data provider plugin needs to be added to the [pom.xml](../certify-service/pom.xml) of the certify-service module.
   ```declarative
   <dependeny>
       <groupId>io.mosip.certify</groupId>
       <artifactId>mock-certify-plugin</artifactId>
       <version>0.5.0</version>
   </dependeny>
   ```
5. Use the CredentialConfig endpoints to add a vc type to local certify issuer. Refer to [Credential-Issuer-Configuration.md](./Credential-Issuer-Configuration.md) and [Mosip Stoplight Documentation](https://mosip.stoplight.io/docs/inji-certify/b27d7165a3af7-add-credential-configuration) for more details. 
6. And for the default config refer to the [certify-local-setup](./postman-collections/certify-local-setup.postman_collection.json).

## Optional
1. Update the `mosip.certify.data-provider-plugin.did-url` to a did url from where did.json can be hosted.
   ```properties
   mosip.certify.data-provider-plugin.did-url=did:web:someuser.github.io:somerepo:somedirectory
   ```
2. (required for VC verification) Certify will automatically generate the DID document for your usecase at [this endpoint](http://localhost:8090/v1/certify/.well-known/did.json), please copy the contents of the HTTP response and host it appropriately in the same location.
   - A did with the ID `did:web:someuser.github.io:somerepo:somedirectory` will have be accessible at `https://someuser.github.io/somerepo/somedirectory/did.json`, i.e. if GitHub Pages is used to host the file, the contents should go in https://github.com/someuser/somerepo/blob/gh-pages/somedirectory/did.json assuming `gh-pages` is the branch for publishing GitHub Pages as per repository settings.
   - To verify if everything is working you can try to resolve the DID via public DID resolvers such as [Uniresolver](https://dev.uniresolver.io/).
3. Update the `didUrl` field of the `credentialConfig` to have the same value as the above property to verify the VC.


## Locally setting up CSV Plugin


The above README can be used to setup the [CSV Plugin](https://github.com/mosip/digital-credential-plugins/tree/develop/mock-certify-plugin) and it'll help showcase how one can setup a custom authored plugin for local testing.

Pre-requisites:

* a working Authorization service which gives an identifiable information in the end-user's ID in the `sub` field
* pre-populated CSV file configured with the matching identities to be authenticated against
