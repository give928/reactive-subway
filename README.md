<p style="text-align: center;">
    <img width="200px;" src="https://raw.githubusercontent.com/woowacourse/atdd-subway-admin-frontend/master/images/main_logo.png" alt="logo"/>
</p>
<p style="text-align: center;">
  <img alt="npm" src="https://img.shields.io/badge/npm-%3E%3D%205.5.0-blue" />
  <img alt="node" src="https://img.shields.io/badge/node-%3E%3D%209.3.0-blue" />
  <img alt="GitHub" src="https://img.shields.io/github/license/next-step/atdd-subway-service" />
</p>

<br>

# Reactive subway

Project: Gradle

Language: Java

Spring Boot: 2.7.2

Project Metadata/Java: 11

Dependencies:
- Spring WebFlux
- Spring Data R2DBC
- Spring AOP
- Validation
- Lombok
- R2DBC PostgreSQL Driver
- R2DBC H2 Database
- jgraph
- jwt
- flyway
- reactor test
- blockhound

<br />

## 🚀 Getting Started

### Install
#### npm
```shell
$ cd frontend
$ npm install
```
#### database(PostgreSQL Master/Slave replication)
```shell
$ cd database/postgres
$ docker-compose up -d
```
#### log directory
```shell
$ mkdir -p /var/log/app
$ sudo chown <user> /var/log/app
```

### Usage
#### run webpack server
```
npm run dev
```
#### run application
```
./gradlew clean build
```

### Spring WebFlux
#### WebFlux Config
```java
@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {
    // ...
}
```

#### Filters
- EtagWebFilter
  ```java
  @Component
  public class EtagWebFilter implements WebFilter {
      @Override
      public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
          // ...
      }
  }
  ```

#### Exceptions
- Annotated Controllers
  ```java
  @RestControllerAdvice(basePackages = {"nextstep.subway"})
  @Slf4j
  public class GlobalRestControllerExceptionHandler {
      @ExceptionHandler(RuntimeException.class)
      public ResponseEntity<Void> handleRuntimeException(RuntimeException e) {
          log.error("handle RuntimeException", e);
          return ResponseEntity.badRequest().build();
      }
  }
  ```
- Functional Endpoints
  ```java
  @Component
  @Order(-2)
  @Slf4j
  public class GlobalFunctionalEndpointsExceptionHandler implements WebExceptionHandler {
      @Override
      public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
          log.error("handle throwable", throwable);
          return response(throwable).flatMap(serverResponse -> serverResponse.writeTo(exchange, handlerStrategiesResponseContext));
      }
  }
  ```

#### Codecs
- Jackson2JsonEncoder
- Jackson2JsonDecoder
```java
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper()));
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper()));
    }

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .failOnUnknownProperties(false)
                .featuresToDisable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .serializerByType(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_DATE))
                .build();
    }
}
```

#### Annotated Controllers
```java
@RequestMapping("/members")
@RestController
public class MemberController {
  @GetMapping("/{id}")
  public Mono<ResponseEntity<MemberResponse>> findMember(@PathVariable Long id) {
    return memberService.findMember(id)
            .map(ResponseEntity::ok);
  }
}
```

#### Functional Endpoints
```java
@Configuration
public class RouterFunctionConfig {
    @Bean
    public RouterFunction<ServerResponse> routeStations(StationHandler stationHandler) {
        return RouterFunctions.route()
                .path("/stations", builder -> builder
                        .GET("", request -> stationHandler.showStations())
                        .POST("", stationHandler::createStation)
                        .GET("/pages", stationHandler::pagingStations)
                        .DELETE("/{id}", stationHandler::deleteStation)
                )
                .build();
    }
}

@Component
@RequiredArgsConstructor
public class StationHandler {
    private final StationService stationService;
    private final RequestValidator requestValidator;

    public Mono<ServerResponse> createStation(ServerRequest request) {
        return request.bodyToMono(StationRequest.class)
                .doOnNext(requestValidator::validate)
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .flatMap(stationService::saveStation)
                .flatMap(stationResponse -> ServerResponse.created(URI.create("/stations/" + stationResponse.getId()))
                        .body(BodyInserters.fromValue(stationResponse)));
    }
}
```

#### Static Resources, CacheControl
```java
@Configuration
public class RouterFunctionConfig {
    @Bean
    public RouterFunction<ServerResponse> routeIndex(@Value("classpath:/templates/index.html") final Resource indexHtml) {
        return RouterFunctions.route()
                .nest(RouterFunctionConfig::isAcceptHtml, builder -> builder
                        .GET("/", request -> response(indexHtml))
                        .GET(STATIONS, request -> response(indexHtml))
                        .GET("/lines", request -> response(indexHtml))
                        .GET("/sections", request -> response(indexHtml))
                        .GET("/path", request -> response(indexHtml))
                        .GET("/login", request -> response(indexHtml))
                        .GET("/join", request -> response(indexHtml))
                        .GET("/mypage", request -> response(indexHtml))
                        .GET("/mypage/edit", request -> response(indexHtml))
                        .GET("/favorites", request -> response(indexHtml)))
                .build();
    }
}

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {
    private static final String STATIC_RESOURCE_PATTERN = "/**";
    static final String STATIC_JS_RESOURCE_PATTERN = "/js/**";
    static final String STATIC_CSS_RESOURCE_PATTERN = "/css/**";
    @Value("${spring.web.resources.static-locations}")
    private String staticLocations;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(STATIC_RESOURCE_PATTERN)
                .addResourceLocations(staticLocations)
                .setCacheControl(CacheControl.noCache().cachePrivate());
        
        registry.addResourceHandler(STATIC_JS_RESOURCE_PATTERN)
                .addResourceLocations(staticLocations + "js/")
                .setCacheControl(CacheControl.noCache().cachePrivate());
        
        registry.addResourceHandler(STATIC_CSS_RESOURCE_PATTERN)
                .addResourceLocations(staticLocations + "css/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
    }
}
```

#### HTTP/2
- certificates
  - hosting provider
  - localhost
    - openssl
      ```shell
      $ openssl req -x509 -out localhost.crt -keyout localhost.key \
        -newkey rsa:2048 -nodes -sha256 \
        -subj '/CN=localhost' -extensions EXT -config <( \
        printf "[dn]\nCN=localhost\n[req]\ndistinguished_name = dn\n[EXT]\nsubjectAltName=DNS:localhost\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth")
      ```
    - minica
      ```shell
      $ brew install minica
      $ minica -domains localhost -ip-addresses 127.0.0.1
      ```
    - [x] `asus router`
      - [http://router.asus.com/](http://router.asus.com/)
      - WAN -> DDNS
      - Enable the DDNS Client: Yes
      - HTTPS/SSL Certificate: Free Certificate from Let's Encrypt
      - Server Certificate: Export
- convert pem to keystore
  ```shell
  $ openssl pkcs12 -export -inkey key.pem -in cert.pem -out give928-keystore.p12
  ```
- application.yml
  ```
  server:
    port: 443
    ssl:
      enabled: true
      key-store: classpath:cert/give928-keystore.p12
      key-store-type: PKCS12
      key-store-password: 
      enabled-protocols: TLSv1.2,TLSv1.3
    http2:
      enabled: true
  ```

#### ConnectionFactoryConfiguration
- 읽기/쓰기 `TransactionManager` 를 각각 사용하도록 설정
  - 마스터 데이터베이스로 연결되는 `writeTransactionManager` 로 쓰기를 하고 기본으로 사용
  - 읽기는 슬레이브 데이터베이스로 연결되는 `readTransactionManager` 를 사용
  - [PostgreSQL](./database/postgres/docker-compose.yml) synchronous_commit 설정
    ```java
    @Service
    @Transactional(readOnly = true, transactionManager = "readTransactionManager")
    public class MemberService {
        @Transactional
        public Mono<MemberResponse> createMember(MemberRequest request) {
            // ...
        }
    }
    ```
  - `@Transactional(readOnly = true)` 로 분기하려 해봤지만 불가? 실패?
    - jdbc
      - `AbstractRoutingDataSource` 구현 클래스를 `LazyConnectionDataSourceProxy` 로 감싸면 런타임 시점에 `determineCurrentLookupKey()` 메서드에 의해 연결할 DataSource 를 결정할 수 있다.
    - r2dbc
      - 일반적인(1 ConnectionFactory, 1 TransactionManager) 설정은 당연히 잘된다.
      
        <details>
        <summary>접기/펼치기</summary>
        
        - 설정
          ```java
          public class ConnectionFactoryConfig extends AbstractR2dbcConfiguration {
              @Bean
              @Override
              public ConnectionFactory connectionFactory() {
                  return ConnectionFactories.get(masterConnectionProperties.getConnectionFactoryOptions());
              }

              @Bean
              public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
                  return new R2dbcTransactionManager(connectionFactory);
              }
          }
          ```
        
        - 읽기
          ```
          DEBUG 56461 --- [ctor-http-nio-3] o.s.r.c.R2dbcTransactionManager          : Creating new transaction with name [nextstep.subway.station.application.StationService.findAllStations]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT,readOnly
          DEBUG 56461 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: SHOW TRANSACTION ISOLATION LEVEL
          DEBUG 56461 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: SELECT oid, typname FROM pg_catalog.pg_type WHERE typname IN ('hstore')
          DEBUG 56461 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Acquired Connection [MonoMap] for R2DBC transaction
          DEBUG 56461 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Switching R2DBC Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@337fd5da, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@794b69e3}] to manual commit
          DEBUG 56461 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: BEGIN
          DEBUG 56461 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: SELECT station.* FROM station
          DEBUG 56461 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Initiating transaction commit
          DEBUG 56461 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Committing R2DBC transaction on Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@337fd5da, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@794b69e3}]
          DEBUG 56461 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: COMMIT
          DEBUG 56461 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Releasing R2DBC Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@337fd5da, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@794b69e3}] after transaction
          ```

        - 쓰기 예외 발생

          ```java
          @Transactional
          public Mono<Void> test() {
              String email = "tester@test.com";
              return memberRepository.findByEmail(email)
                      .flatMap(member -> memberRepository.save(member.update(Member.builder()
                                                                                     .email(email)
                                                                                     .password(member.getPassword())
                                                                                     .age(member.getAge() + 1)
                                                                                     .build())))
                      .switchIfEmpty(memberRepository.save(Member.builder()
                                                                   .email(email)
                                                                   .password("123456")
                                                                   .age(10)
                                                                   .build()))
                      .map(member -> {
                          if (member.getEmail() != null) {
                              throw new RuntimeException("force exception!!");
                          }
                          return member;
                      })
                      .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                      .then();
          }
          ```

          ```
          DEBUG 56461 --- [ctor-http-nio-4] o.s.r.c.R2dbcTransactionManager          : Creating new transaction with name [nextstep.subway.member.application.MemberService.test]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
          DEBUG 56461 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: SHOW TRANSACTION ISOLATION LEVEL
          DEBUG 56461 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: SELECT oid, typname FROM pg_catalog.pg_type WHERE typname IN ('hstore')
          DEBUG 56461 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Acquired Connection [MonoMap] for R2DBC transaction
          DEBUG 56461 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Switching R2DBC Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@7e9e81f4, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@6c740d4f}] to manual commit
          DEBUG 56461 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: BEGIN
          DEBUG 56461 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: SELECT member.id, member.email, member.password, member.age, member.created_date, member.modified_date FROM member WHERE member.email = $1
          DEBUG 56461 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: UPDATE member SET email = $1, password = $2, age = $3, created_date = $4, modified_date = $5 WHERE member.id = $6
          DEBUG 56461 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Initiating transaction rollback
          DEBUG 56461 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Rolling back R2DBC transaction on Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@7e9e81f4, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@6c740d4f}]
          DEBUG 56461 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: ROLLBACK
          DEBUG 56461 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Releasing R2DBC Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@7e9e81f4, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@6c740d4f}] after transaction
          ERROR 56461 --- [actor-tcp-nio-2] c.a.GlobalRestControllerExceptionHandler : handle RuntimeException
          
          java.lang.IllegalStateException: force exception!!
              at nextstep.subway.member.application.MemberService.lambda$test$4(MemberService.java:70)
              Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
          Error has been observed at the following site(s):
              *__checkpoint ⇢ Handler nextstep.subway.member.ui.MemberController#test() [DispatcherHandler]
          Original Stack Trace:
                  ...
          ```
  
        </details>

      - `AbstractRoutingConnectionFactory` 구현만으로는 분기되지 않는다. master 로만 연결된다.
        - `TransactionSynchronizationManager` 에서 관리되지 않아서 현재 트랜잭션 정보를 확인할 수 없다.
        <details>
        <summary>접기/펼치기</summary>

        - 설정
          ```java
          public class DynamicRoutingConnectionFactory extends AbstractRoutingConnectionFactory {
              @Override
              protected Mono<Object> determineCurrentLookupKey() {
                  return TransactionSynchronizationManager.forCurrentTransaction()
                          .map(synchronizationManager -> {
                              if (synchronizationManager.isActualTransactionActive() && synchronizationManager.isCurrentTransactionReadOnly()) {
                                  return SLAVE;
                              }
                              return MASTER;
                  });
              }
          }

          public class ConnectionFactoryConfig extends AbstractR2dbcConfiguration {
              @Bean
              @Override
              public ConnectionFactory connectionFactory() {
                  return new DynamicRoutingConnectionFactory(
                          ConnectionFactories.get(masterConnectionProperties.getConnectionFactoryOptions()),
                          ConnectionFactories.get(slaveConnectionProperties.getConnectionFactoryOptions()));
              }

              @Bean
              public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
                  return new R2dbcTransactionManager(connectionFactory);
              }
          }
          ```
        
        - 읽기
          ```
          DEBUG 56657 --- [ctor-http-nio-2] o.s.r.c.R2dbcTransactionManager          : Creating new transaction with name [nextstep.subway.station.application.StationService.findAllStations]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT,readOnly
          DEBUG 56657 --- [ctor-http-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isActualTransactionActive() = false
          DEBUG 56657 --- [ctor-http-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isSynchronizationActive() = false
          DEBUG 56657 --- [ctor-http-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isCurrentTransactionReadOnly() = false
          DEBUG 56657 --- [ctor-http-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : RoutingConnectionFactory: master
          DEBUG 56657 --- [       Thread-6] n.s.common.config.web.EtagWebFilter      : resource: class path resource [static/favicon.ico], etag: 06c0afd2c8a965f6deedee4aa4b2ed6d
          DEBUG 56657 --- [       Thread-6] n.s.common.config.web.EtagWebFilter      : class path resource [static/favicon.ico] response 302 not modified
          DEBUG 56657 --- [actor-tcp-nio-3] io.r2dbc.postgresql.QUERY                : Executing query: SHOW TRANSACTION ISOLATION LEVEL
          DEBUG 56657 --- [actor-tcp-nio-3] io.r2dbc.postgresql.QUERY                : Executing query: SELECT oid, typname FROM pg_catalog.pg_type WHERE typname IN ('hstore')
          DEBUG 56657 --- [actor-tcp-nio-3] o.s.r.c.R2dbcTransactionManager          : Acquired Connection [MonoFlatMap] for R2DBC transaction
          DEBUG 56657 --- [actor-tcp-nio-3] o.s.r.c.R2dbcTransactionManager          : Switching R2DBC Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@f8ed765, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@33a50c3f}] to manual commit
          DEBUG 56657 --- [actor-tcp-nio-3] io.r2dbc.postgresql.QUERY                : Executing query: BEGIN
          DEBUG 56657 --- [actor-tcp-nio-3] io.r2dbc.postgresql.QUERY                : Executing query: SELECT station.* FROM station
          DEBUG 56657 --- [actor-tcp-nio-3] o.s.r.c.R2dbcTransactionManager          : Initiating transaction commit
          DEBUG 56657 --- [actor-tcp-nio-3] o.s.r.c.R2dbcTransactionManager          : Committing R2DBC transaction on Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@f8ed765, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@33a50c3f}]
          DEBUG 56657 --- [actor-tcp-nio-3] io.r2dbc.postgresql.QUERY                : Executing query: COMMIT
          DEBUG 56657 --- [actor-tcp-nio-3] o.s.r.c.R2dbcTransactionManager          : Releasing R2DBC Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@f8ed765, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@33a50c3f}] after transaction
          ```

        - 쓰기 예외 발생
          ```
          DEBUG 56657 --- [ctor-http-nio-4] o.s.r.c.R2dbcTransactionManager          : Creating new transaction with name [nextstep.subway.member.application.MemberService.test]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
          DEBUG 56657 --- [ctor-http-nio-4] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isActualTransactionActive() = false
          DEBUG 56657 --- [ctor-http-nio-4] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isSynchronizationActive() = false
          DEBUG 56657 --- [ctor-http-nio-4] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isCurrentTransactionReadOnly() = false
          DEBUG 56657 --- [ctor-http-nio-4] .s.c.c.d.DynamicRoutingConnectionFactory : RoutingConnectionFactory: master
          DEBUG 56657 --- [actor-tcp-nio-4] io.r2dbc.postgresql.QUERY                : Executing query: SHOW TRANSACTION ISOLATION LEVEL
          DEBUG 56657 --- [actor-tcp-nio-4] io.r2dbc.postgresql.QUERY                : Executing query: SELECT oid, typname FROM pg_catalog.pg_type WHERE typname IN ('hstore')
          DEBUG 56657 --- [actor-tcp-nio-4] o.s.r.c.R2dbcTransactionManager          : Acquired Connection [MonoFlatMap] for R2DBC transaction
          DEBUG 56657 --- [actor-tcp-nio-4] o.s.r.c.R2dbcTransactionManager          : Switching R2DBC Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@62c50bb6, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@30fad09e}] to manual commit
          DEBUG 56657 --- [actor-tcp-nio-4] io.r2dbc.postgresql.QUERY                : Executing query: BEGIN
          DEBUG 56657 --- [actor-tcp-nio-4] io.r2dbc.postgresql.QUERY                : Executing query: SELECT member.id, member.email, member.password, member.age, member.created_date, member.modified_date FROM member WHERE member.email = $1
          DEBUG 56657 --- [actor-tcp-nio-4] io.r2dbc.postgresql.QUERY                : Executing query: UPDATE member SET email = $1, password = $2, age = $3, created_date = $4, modified_date = $5 WHERE member.id = $6
          DEBUG 56657 --- [actor-tcp-nio-4] o.s.r.c.R2dbcTransactionManager          : Initiating transaction rollback
          DEBUG 56657 --- [actor-tcp-nio-4] o.s.r.c.R2dbcTransactionManager          : Rolling back R2DBC transaction on Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@62c50bb6, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@30fad09e}]
          DEBUG 56657 --- [actor-tcp-nio-4] io.r2dbc.postgresql.QUERY                : Executing query: ROLLBACK
          DEBUG 56657 --- [actor-tcp-nio-4] o.s.r.c.R2dbcTransactionManager          : Releasing R2DBC Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@62c50bb6, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@30fad09e}] after transaction
          ERROR 56657 --- [actor-tcp-nio-4] c.a.GlobalRestControllerExceptionHandler : handle RuntimeException
          
          java.lang.IllegalStateException: force exception!!
              at nextstep.subway.member.application.MemberService.lambda$test$4(MemberService.java:70)
              Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
          Error has been observed at the following site(s):
              *__checkpoint ⇢ Handler nextstep.subway.member.ui.MemberController#test() [DispatcherHandler]
          Original Stack Trace:
                  ...
          ```
  
        </details>

      - `AbstractRoutingConnectionFactory` 를 `TransactionAwareConnectionFactoryProxy` 로 감싸면 분기가 되지만 문제가 있다.
        - 최초에 `TransactionSynchronizationManager` 에서 관리되지 않는 기본 `ConnectionFactory` 에서 커넥션에서 트랜잭션이 실행된다.
        - 이후에 `TransactionSynchronizationManager` 에서 관리되는 분기된 `ConnectionFactory` 에서 별도의 커넥션에서 sql 이 실행된다.
        - 예외가 발생하면 1번째 트랜잭션에서 롤백 명령이 실행되지만, 2번 커넥션에서 sql 이 실행되었기 때문에 결국 원하는 대로 롤백이 되지 않는다.
        - `TransactionAwareConnectionFactoryProxy` 는 `ConnectionFactory` 를 타겟으로 하는 프록시로 스프링이 관리하는 트랜잭션을 인식할 수 있게 해준다.
          - 이 클래스는 스프링의 R2DBC 기능과 통합되지 않은 R2DBC 클라이언트를 사용할 때 필요하다.
          - 공식문서에서는 가능하다면 대상 `ConnectionFactory` 에 대한 프록시 없이도 트랜잭션 참여를 얻기 위해 Spring 의 `ConnectionFactoryUtils` 또는 `DatabaseClient` 를 사용하여 애초에 이러한 프록시를 정의할 필요가 없도록 하는 것을 권장한다고 한다.
          <details>
          <summary>접기/펼치기</summary>
          
          - 설정
            ```java
            public class ConnectionFactoryConfig extends AbstractR2dbcConfiguration {
                @Bean
                @Override
                public ConnectionFactory connectionFactory() {
                    return new DynamicRoutingConnectionFactory(
                            ConnectionFactories.get(masterConnectionProperties.getConnectionFactoryOptions()),
                            ConnectionFactories.get(slaveConnectionProperties.getConnectionFactoryOptions()));
                }
          
                @Bean
                public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
                    return new R2dbcTransactionManager(new TransactionAwareConnectionFactoryProxy(connectionFactory));
                }
            }
            ```
          
          - 읽기
            ```
            DEBUG 56925 --- [ctor-http-nio-2] o.s.r.c.R2dbcTransactionManager          : Creating new transaction with name [nextstep.subway.station.application.StationService.findAllStations]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT,readOnly
            DEBUG 56925 --- [ctor-http-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isActualTransactionActive() = false
            DEBUG 56925 --- [ctor-http-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isSynchronizationActive() = false
            DEBUG 56925 --- [ctor-http-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isCurrentTransactionReadOnly() = false
            DEBUG 56925 --- [ctor-http-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : RoutingConnectionFactory: master
            DEBUG 56925 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: SHOW TRANSACTION ISOLATION LEVEL
            DEBUG 56925 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: SELECT oid, typname FROM pg_catalog.pg_type WHERE typname IN ('hstore')
            DEBUG 56925 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Acquired Connection [MonoMap] for R2DBC transaction
            DEBUG 56925 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Switching R2DBC Connection [Transaction-aware proxy for target Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@4e641ca, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@1012431e}]] to manual commit
            DEBUG 56925 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: BEGIN
            DEBUG 56925 --- [actor-tcp-nio-1] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isActualTransactionActive() = true
            DEBUG 56925 --- [actor-tcp-nio-1] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isSynchronizationActive() = true
            DEBUG 56925 --- [actor-tcp-nio-1] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isCurrentTransactionReadOnly() = true
            DEBUG 56925 --- [actor-tcp-nio-1] .s.c.c.d.DynamicRoutingConnectionFactory : RoutingConnectionFactory: slave
            DEBUG 56925 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: SHOW TRANSACTION ISOLATION LEVEL
            DEBUG 56925 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: SELECT oid, typname FROM pg_catalog.pg_type WHERE typname IN ('hstore')
            DEBUG 56925 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: SELECT station.* FROM station
            DEBUG 56925 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Initiating transaction commit
            DEBUG 56925 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Committing R2DBC transaction on Connection [Transaction-aware proxy for target Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@4e641ca, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@1012431e}]]
            DEBUG 56925 --- [actor-tcp-nio-1] io.r2dbc.postgresql.QUERY                : Executing query: COMMIT
            DEBUG 56925 --- [actor-tcp-nio-1] o.s.r.c.R2dbcTransactionManager          : Releasing R2DBC Connection [Transaction-aware proxy for target Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@4e641ca, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@1012431e}]] after transaction
            ```
          
          - 쓰기 예외 발생
            ```
            DEBUG 56925 --- [ctor-http-nio-4] o.s.r.c.R2dbcTransactionManager          : Creating new transaction with name [nextstep.subway.member.application.MemberService.test]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
            DEBUG 56925 --- [ctor-http-nio-4] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isActualTransactionActive() = false
            DEBUG 56925 --- [ctor-http-nio-4] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isSynchronizationActive() = false
            DEBUG 56925 --- [ctor-http-nio-4] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isCurrentTransactionReadOnly() = false
            DEBUG 56925 --- [ctor-http-nio-4] .s.c.c.d.DynamicRoutingConnectionFactory : RoutingConnectionFactory: master
            DEBUG 56925 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: SHOW TRANSACTION ISOLATION LEVEL
            DEBUG 56925 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: SELECT oid, typname FROM pg_catalog.pg_type WHERE typname IN ('hstore')
            DEBUG 56925 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Acquired Connection [MonoMap] for R2DBC transaction
            DEBUG 56925 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Switching R2DBC Connection [Transaction-aware proxy for target Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@4ad38cc8, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@73d521f7}]] to manual commit
            DEBUG 56925 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: BEGIN
            DEBUG 56925 --- [actor-tcp-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isActualTransactionActive() = true
            DEBUG 56925 --- [actor-tcp-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isSynchronizationActive() = true
            DEBUG 56925 --- [actor-tcp-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : synchronizationManager.isCurrentTransactionReadOnly() = false
            DEBUG 56925 --- [actor-tcp-nio-2] .s.c.c.d.DynamicRoutingConnectionFactory : RoutingConnectionFactory: master
            DEBUG 56925 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: SHOW TRANSACTION ISOLATION LEVEL
            DEBUG 56925 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: SELECT oid, typname FROM pg_catalog.pg_type WHERE typname IN ('hstore')
            DEBUG 56925 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: SELECT member.id, member.email, member.password, member.age, member.created_date, member.modified_date FROM member WHERE member.email = $1
            DEBUG 56925 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: UPDATE member SET email = $1, password = $2, age = $3, created_date = $4, modified_date = $5 WHERE member.id = $6
            DEBUG 56925 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Initiating transaction rollback
            DEBUG 56925 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Rolling back R2DBC transaction on Connection [Transaction-aware proxy for target Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@4ad38cc8, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@73d521f7}]]
            DEBUG 56925 --- [actor-tcp-nio-2] io.r2dbc.postgresql.QUERY                : Executing query: ROLLBACK
            DEBUG 56925 --- [actor-tcp-nio-2] o.s.r.c.R2dbcTransactionManager          : Releasing R2DBC Connection [Transaction-aware proxy for target Connection [PostgresqlConnection{master, client=io.r2dbc.postgresql.client.ReactorNettyClient@4ad38cc8, codecs=io.r2dbc.postgresql.codec.DefaultCodecs@73d521f7}]] after transaction
            ERROR 56925 --- [actor-tcp-nio-2] c.a.GlobalRestControllerExceptionHandler : handle RuntimeException
          
            java.lang.IllegalStateException: force exception!!
                at nextstep.subway.member.application.MemberService.lambda$test$4(MemberService.java:70)
                Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
            Error has been observed at the following site(s):
                *__checkpoint ⇢ Handler nextstep.subway.member.ui.MemberController#test() [DispatcherHandler]
            Original Stack Trace:
                    ...
            ```
          
          </details>

        - 또한 트랜잭션 내에서 publisher 를 결합(Mono.zip, mono.zipWith, mono.zipWhen, Mono.when ...)할 때 SQL 을 2회 이상 실행하면 예외가 발생한다.
          - 동일한 스레드에서 순서대로 실행되는 것 같은데 왜?
            - SQL 이 실행될 때 마다 새로운 커넥션으로 연결하는 것 같다. 2번째 SQL 이 실행될 때 `TransactionSynchronizationManager` `ConnectionHolder` 에 이미 이전 커넥션이 있어서 예외가 발생한다.
          - 순차적으로 실행하면(첫 번째 반환 값을 flatMap 으로 변형할 때 두 번째 반환 값과 함께 Tuple 로 반환) 동일한 커넥션에서 실행되는지 예외가 발생하지 않는다.
          <details>
          <summary>접기/펼치기</summary>
      
          ```java
          // 예외 발생함
          public Flux<Line> test() {
              return lineRepository.findAll()
                      .collectList()
                      .zipWith(sectionRepository.findAll())
                      //...
                      .flatMapMany(Flux::fromStream);
          }
          
          // 예외 발생하지 않음
          public Flux<Line> test() {
              return lineRepository.findAll()
                      .collectList()
                      .flatMap(lines -> sectionRepository.findAll()
                              .flatMap(sections -> Mono.just(Tuples.of(lines, sections))))
                      //...
                      .flatMapMany(Flux::fromStream);
          }
          ```
      
          ```
          org.springframework.dao.DataAccessResourceFailureException: Failed to obtain R2DBC Connection; nested exception is java.lang.IllegalStateException: Already value [org.springframework.r2dbc.connection.ConnectionHolder@33bee026] for key [PostgresqlConnectionFactory{clientFactory=io.r2dbc.postgresql.PostgresqlConnectionFactory$$Lambda$742/0x0000000800656040@34738205, configuration=PostgresqlConnectionConfiguration{applicationName='r2dbc-postgresql', autodetectExtensions='true', compatibilityMode=false, connectTimeout=null, database='subway', extensions=[], fetchSize=io.r2dbc.postgresql.PostgresqlConnectionConfiguration$Builder$$Lambda$688/0x0000000800648840@21bdd054, forceBinary='false', host='127.0.0.1', lockWaitTimeout='null, loopResources='null', options='{}', password='***************', port=5432, preferAttachedBuffers=false, socket=null, statementTimeout=null, tcpKeepAlive=false, tcpNoDelay=true, username='subway'}, extensions=io.r2dbc.postgresql.Extensions@25befc63}] bound to context
              at org.springframework.r2dbc.connection.ConnectionFactoryUtils.lambda$getConnection$0(ConnectionFactoryUtils.java:88)
              Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
          Error has been observed at the following site(s):
              *__checkpoint ⇢ Handler nextstep.subway.member.ui.MemberController#test() [DispatcherHandler]
          Original Stack Trace:
                  at org.springframework.r2dbc.connection.ConnectionFactoryUtils.lambda$getConnection$0(ConnectionFactoryUtils.java:88)
                  at reactor.core.publisher.Mono.lambda$onErrorMap$31(Mono.java:3730)
                  at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:94)
                  at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:106)
                  at reactor.core.publisher.Operators.error(Operators.java:198)
                  at reactor.core.publisher.MonoError.subscribe(MonoError.java:53)
                  at reactor.core.publisher.Mono.subscribe(Mono.java:4397)
                  at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:103)
                  at reactor.core.publisher.MonoFlatMap$FlatMapMain.secondError(MonoFlatMap.java:192)
                  at reactor.core.publisher.MonoFlatMap$FlatMapInner.onError(MonoFlatMap.java:259)
                  at reactor.core.publisher.MonoFlatMap$FlatMapMain.secondError(MonoFlatMap.java:192)
                  at reactor.core.publisher.MonoFlatMap$FlatMapInner.onError(MonoFlatMap.java:259)
                  at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:106)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onError(MonoIgnoreThen.java:278)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:231)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:203)
                  at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.onComplete(Operators.java:2058)
                  at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.onComplete(Operators.java:2058)
                  at reactor.core.publisher.MonoFlatMap$FlatMapMain.secondComplete(MonoFlatMap.java:196)
                  at reactor.core.publisher.MonoFlatMap$FlatMapInner.onComplete(MonoFlatMap.java:268)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:209)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:238)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:203)
                  at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:209)
                  at reactor.core.publisher.Operators.complete(Operators.java:137)
                  at reactor.netty.FutureMono.doSubscribe(FutureMono.java:122)
                  at reactor.netty.FutureMono$ImmediateFutureMono.subscribe(FutureMono.java:83)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:240)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:203)
                  at reactor.core.publisher.MonoPeekTerminal$MonoTerminalPeekSubscriber.onComplete(MonoPeekTerminal.java:299)
                  at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
                  at reactor.core.publisher.FluxConcatMap$ConcatMapImmediate.drain(FluxConcatMap.java:368)
                  at reactor.core.publisher.FluxConcatMap$ConcatMapImmediate.onComplete(FluxConcatMap.java:276)
                  at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onComplete(FluxPeekFuseable.java:277)
                  at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2400)
                  at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.request(FluxPeekFuseable.java:144)
                  at reactor.core.publisher.FluxConcatMap$ConcatMapImmediate.onSubscribe(FluxConcatMap.java:236)
                  at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onSubscribe(FluxPeekFuseable.java:178)
                  at reactor.core.publisher.FluxJust.subscribe(FluxJust.java:68)
                  at reactor.core.publisher.Mono.subscribe(Mono.java:4397)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:263)
                  at reactor.core.publisher.MonoIgnoreThen.subscribe(MonoIgnoreThen.java:51)
                  at reactor.core.publisher.MonoDefer.subscribe(MonoDefer.java:52)
                  at reactor.core.publisher.Mono.subscribe(Mono.java:4397)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:263)
                  at reactor.core.publisher.MonoIgnoreThen.subscribe(MonoIgnoreThen.java:51)
                  at reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:157)
                  at reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:122)
                  at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2398)
                  at reactor.core.publisher.FluxMap$MapSubscriber.request(FluxMap.java:164)
                  at reactor.core.publisher.MonoFlatMap$FlatMapMain.onSubscribe(MonoFlatMap.java:110)
                  at reactor.core.publisher.FluxMap$MapSubscriber.onSubscribe(FluxMap.java:92)
                  at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55)
                  at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:55)
                  at reactor.core.publisher.Mono.subscribe(Mono.java:4397)
                  at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:263)
                  at reactor.core.publisher.MonoIgnoreThen.subscribe(MonoIgnoreThen.java:51)
                  at reactor.core.publisher.Mono.subscribe(Mono.java:4397)
                  at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:103)
                  at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onError(FluxPeekFuseable.java:234)
                  at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:205)
                  at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2398)
                  at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.request(FluxPeekFuseable.java:144)
                  at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.set(Operators.java:2194)
                  at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onSubscribe(FluxOnErrorResume.java:74)
                  at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onSubscribe(FluxPeekFuseable.java:178)
                  at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55)
                  at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:64)
                  at reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:157)
                  at reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:122)
                  at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onNext(FluxOnErrorResume.java:79)
                  at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1816)
                  at reactor.core.publisher.MonoFlatMap$FlatMapInner.onNext(MonoFlatMap.java:249)
                  at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onNext(FluxOnErrorResume.java:79)
                  at reactor.core.publisher.MonoDelayUntil$DelayUntilCoordinator.complete(MonoDelayUntil.java:418)
                  at reactor.core.publisher.MonoDelayUntil$DelayUntilTrigger.onComplete(MonoDelayUntil.java:531)
                  at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
                  at reactor.core.publisher.FluxConcatIterable$ConcatIterableSubscriber.onComplete(FluxConcatIterable.java:121)
                  at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
                  at reactor.core.publisher.FluxFlatMap$FlatMapMain.checkTerminated(FluxFlatMap.java:846)
                  at reactor.core.publisher.FluxFlatMap$FlatMapMain.drainLoop(FluxFlatMap.java:608)
                  at reactor.core.publisher.FluxFlatMap$FlatMapMain.drain(FluxFlatMap.java:588)
                  at reactor.core.publisher.FluxFlatMap$FlatMapMain.onComplete(FluxFlatMap.java:465)
                  at io.r2dbc.postgresql.util.FluxDiscardOnCancel$FluxDiscardOnCancelSubscriber.onComplete(FluxDiscardOnCancel.java:99)
                  at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onComplete(FluxMapFuseable.java:152)
                  at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.checkTerminated(FluxWindowPredicate.java:538)
                  at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.drainLoop(FluxWindowPredicate.java:486)
                  at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.drain(FluxWindowPredicate.java:430)
                  at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.onComplete(FluxWindowPredicate.java:310)
                  at io.r2dbc.postgresql.util.FluxDiscardOnCancel$FluxDiscardOnCancelSubscriber.onComplete(FluxDiscardOnCancel.java:99)
                  at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onComplete(FluxContextWrite.java:126)
                  at reactor.core.publisher.FluxCreate$BaseSink.complete(FluxCreate.java:460)
                  at reactor.core.publisher.FluxCreate$BufferAsyncSink.drain(FluxCreate.java:805)
                  at reactor.core.publisher.FluxCreate$BufferAsyncSink.complete(FluxCreate.java:753)
                  at reactor.core.publisher.FluxCreate$SerializedFluxSink.drainLoop(FluxCreate.java:247)
                  at reactor.core.publisher.FluxCreate$SerializedFluxSink.drain(FluxCreate.java:213)
                  at reactor.core.publisher.FluxCreate$SerializedFluxSink.complete(FluxCreate.java:204)
                  at io.r2dbc.postgresql.client.ReactorNettyClient$Conversation.complete(ReactorNettyClient.java:638)
                  at io.r2dbc.postgresql.client.ReactorNettyClient$BackendMessageSubscriber.emit(ReactorNettyClient.java:904)
                  at io.r2dbc.postgresql.client.ReactorNettyClient$BackendMessageSubscriber.onNext(ReactorNettyClient.java:780)
                  at io.r2dbc.postgresql.client.ReactorNettyClient$BackendMessageSubscriber.onNext(ReactorNettyClient.java:686)
                  at reactor.core.publisher.FluxHandle$HandleSubscriber.onNext(FluxHandle.java:126)
                  at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onNext(FluxPeekFuseable.java:854)
                  at reactor.core.publisher.FluxMap$MapConditionalSubscriber.onNext(FluxMap.java:224)
                  at reactor.core.publisher.FluxMap$MapConditionalSubscriber.onNext(FluxMap.java:224)
                  at reactor.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:279)
                  at reactor.netty.channel.FluxReceive.onInboundNext(FluxReceive.java:388)
                  at reactor.netty.channel.ChannelOperations.onInboundNext(ChannelOperations.java:404)
                  at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:93)
                  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
                  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
                  at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
                  at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:327)
                  at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:299)
                  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
                  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
                  at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
                  at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1410)
                  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
                  at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
                  at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:919)
                  at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:166)
                  at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:722)
                  at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:658)
                  at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:584)
                  at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:496)
                  at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
                  at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
                  at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
                  at java.base/java.lang.Thread.run(Thread.java:829)
          Caused by: java.lang.IllegalStateException: Already value [org.springframework.r2dbc.connection.ConnectionHolder@33bee026] for key [PostgresqlConnectionFactory{clientFactory=io.r2dbc.postgresql.PostgresqlConnectionFactory$$Lambda$742/0x0000000800656040@34738205, configuration=PostgresqlConnectionConfiguration{applicationName='r2dbc-postgresql', autodetectExtensions='true', compatibilityMode=false, connectTimeout=null, database='subway', extensions=[], fetchSize=io.r2dbc.postgresql.PostgresqlConnectionConfiguration$Builder$$Lambda$688/0x0000000800648840@21bdd054, forceBinary='false', host='127.0.0.1', lockWaitTimeout='null, loopResources='null', options='{}', password='***************', port=5432, preferAttachedBuffers=false, socket=null, statementTimeout=null, tcpKeepAlive=false, tcpNoDelay=true, username='subway'}, extensions=io.r2dbc.postgresql.Extensions@25befc63}] bound to context
              at org.springframework.transaction.reactive.TransactionSynchronizationManager.bindResource(TransactionSynchronizationManager.java:134)
              at org.springframework.r2dbc.connection.ConnectionFactoryUtils.lambda$null$1(ConnectionFactoryUtils.java:131)
              at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:196)
              at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2398)
              at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.request(FluxPeekFuseable.java:144)
              at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.set(Operators.java:2194)
              at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onSubscribe(FluxOnErrorResume.java:74)
              at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onSubscribe(FluxPeekFuseable.java:178)
              at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55)
              at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:64)
              at reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:157)
              at reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:122)
              at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onNext(FluxOnErrorResume.java:79)
              at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1816)
              at reactor.core.publisher.MonoFlatMap$FlatMapInner.onNext(MonoFlatMap.java:249)
              at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onNext(FluxOnErrorResume.java:79)
              at reactor.core.publisher.MonoDelayUntil$DelayUntilCoordinator.complete(MonoDelayUntil.java:418)
              at reactor.core.publisher.MonoDelayUntil$DelayUntilTrigger.onComplete(MonoDelayUntil.java:531)
              at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
              at reactor.core.publisher.FluxConcatIterable$ConcatIterableSubscriber.onComplete(FluxConcatIterable.java:121)
              at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
              at reactor.core.publisher.FluxFlatMap$FlatMapMain.checkTerminated(FluxFlatMap.java:846)
              at reactor.core.publisher.FluxFlatMap$FlatMapMain.drainLoop(FluxFlatMap.java:608)
              at reactor.core.publisher.FluxFlatMap$FlatMapMain.drain(FluxFlatMap.java:588)
              at reactor.core.publisher.FluxFlatMap$FlatMapMain.onComplete(FluxFlatMap.java:465)
              at io.r2dbc.postgresql.util.FluxDiscardOnCancel$FluxDiscardOnCancelSubscriber.onComplete(FluxDiscardOnCancel.java:99)
              at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onComplete(FluxMapFuseable.java:152)
              at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.checkTerminated(FluxWindowPredicate.java:538)
              at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.drainLoop(FluxWindowPredicate.java:486)
              at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.drain(FluxWindowPredicate.java:430)
              at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.onComplete(FluxWindowPredicate.java:310)
              at io.r2dbc.postgresql.util.FluxDiscardOnCancel$FluxDiscardOnCancelSubscriber.onComplete(FluxDiscardOnCancel.java:99)
              at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onComplete(FluxContextWrite.java:126)
              at reactor.core.publisher.FluxCreate$BaseSink.complete(FluxCreate.java:460)
              at reactor.core.publisher.FluxCreate$BufferAsyncSink.drain(FluxCreate.java:805)
              at reactor.core.publisher.FluxCreate$BufferAsyncSink.complete(FluxCreate.java:753)
              at reactor.core.publisher.FluxCreate$SerializedFluxSink.drainLoop(FluxCreate.java:247)
              at reactor.core.publisher.FluxCreate$SerializedFluxSink.drain(FluxCreate.java:213)
              at reactor.core.publisher.FluxCreate$SerializedFluxSink.complete(FluxCreate.java:204)
              at io.r2dbc.postgresql.client.ReactorNettyClient$Conversation.complete(ReactorNettyClient.java:638)
              at io.r2dbc.postgresql.client.ReactorNettyClient$BackendMessageSubscriber.emit(ReactorNettyClient.java:904)
              at io.r2dbc.postgresql.client.ReactorNettyClient$BackendMessageSubscriber.onNext(ReactorNettyClient.java:780)
              at io.r2dbc.postgresql.client.ReactorNettyClient$BackendMessageSubscriber.onNext(ReactorNettyClient.java:686)
              at reactor.core.publisher.FluxHandle$HandleSubscriber.onNext(FluxHandle.java:126)
              at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onNext(FluxPeekFuseable.java:854)
              at reactor.core.publisher.FluxMap$MapConditionalSubscriber.onNext(FluxMap.java:224)
              at reactor.core.publisher.FluxMap$MapConditionalSubscriber.onNext(FluxMap.java:224)
              at reactor.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:279)
              at reactor.netty.channel.FluxReceive.onInboundNext(FluxReceive.java:388)
              at reactor.netty.channel.ChannelOperations.onInboundNext(ChannelOperations.java:404)
              at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:93)
              at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
              at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
              at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
              at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:327)
              at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:299)
              at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
              at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
              at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
              at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1410)
              at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
              at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
              at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:919)
              at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:166)
              at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:722)
              at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:658)
              at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:584)
              at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:496)
              at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
              at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
              at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
              at java.base/java.lang.Thread.run(Thread.java:829)
          ```
          
          </details>
