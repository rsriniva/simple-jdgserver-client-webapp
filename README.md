#Making REST calls

## Initializing the Cache Manager

### Without SSL and without Authentication

```
PUT http://127.0.0.1:8180/simple-jdgserver-client-webapp/initialize?ssl=false&authenticate=false
127.0.0.1:11222
```

### Without SSL and with Authentication

```
PUT http://127.0.0.1:8180/simple-jdgserver-client-webapp/initialize?ssl=false&authenticate=true&username=jdgClientUser&password=jdgClientPass123!
127.0.0.1:11222
```

### With SSL and without Authentication

```
PUT http://127.0.0.1:8180/simple-jdgserver-client-webapp/initialize?ssl=true&authenticate=false
127.0.0.1:11222
```

### With SSL and with Authentication

```
PUT http://127.0.0.1:8180/simple-jdgserver-client-webapp/initialize?ssl=true&authenticate=true&username=jdgClientUser&password=jdgClientPass123!
127.0.0.1:11222
```

## Making a PUT into Cache

```
PUT http://127.0.0.1:8180/simple-jdgserver-client-webapp/namedCache/1
One
```

## Making a GET from Cache

```
GET http://127.0.0.1:8180/simple-jdgserver-client-webapp/namedCache/1
```

