## SSL for JDG in Client-Server mode

### Create a JDG Server Keystore

```sh
keytool -genkeypair -keyalg RSA -alias jdg-server -keystore jdg-server.keystore --dname "CN=Vijay Chintalapati,OU=Middleware Specialist,O=Red Hat,L=Raleigh,S=NC,C=US"
```

Use password: $`jdg-server^Pas$` for the key and the store

### Export JDG Server Certificate

```sh
keytool -export -alias jdg-server -keystore jdg-server.keystore -file jdg-server.crt
```

(same password as above for the server keystore)

### Create a JDG Client Keystore

```sh
keytool -genkeypair -keyalg RSA -alias jdg-client -keystore jdg-client.keystore --dname "CN=Vijay Chintalapati,OU=Middleware Specialist,O=Red Hat,L=Raleigh,S=NC,C=US"
```

Use password: `jdg-client^Pas$` for the key and the store

### Export JDG Client Certificate

```sh
keytool -export -alias jdg-client -keystore jdg-client.keystore -file jdg-client.crt
```

(same password as above for client keystore)


### Import Client Certificate into Server TrustStore

```sh
keytool -importcert -alias jdg-client -keystore jdg-server.truststore -file jdg-client.crt
```

Use password: `jdg-server-trust^Pas$`

### Import Server Certificate into Client TrustStore

```sh
keytool -importcert -alias jdg-server -keystore jdg-client.truststore -file jdg-server.crt
```
Use password: `jdg-client-trust^Pas$`

### Final Instruction 

Copy the server .keystore and .truststore to server location ideally to the `${jboss.server.config.dir}` folder of the JDG server(s)

Copy the client .keystore and .truststore to the client location(s) and reference their path in the client application