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
- Spring Reactive Web
- Spring Data R2DBC
- Spring AOP
- Validation
- Lombok
- R2DBC MySQL Driver
- R2DBC H2 Database
- jgraph
- jwt
- flyway
- reactor test
- blockhound
- rest assured

<br />

## 🚀 Getting Started

### Install
#### npm
```shell
$ cd frontend
$ npm install
```
#### database(MySQL Master/Slave replication)
```shell
$ cd database/mysql8
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
      enabled-protocols: TLSv1.3
    http2:
      enabled: true
  ```

#### ConnectionFactoryConfiguration
- DynamicRoutingConnectionFactory extends AbstractRoutingConnectionFactory
  - `@Override protected Mono<Object> determineCurrentLookupKey`
    - 트랜잭션이 readOnly = true 이면 slave 로 연결
    - 그 외에 master 로 연결
```java
@Profile({"local", "prod"})
@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties
@EnableTransactionManagement
public class ConnectionFactoryConfig extends AbstractR2dbcConfiguration {
    private final MasterConnectionProperties masterConnectionProperties;
    private final SlaveConnectionProperties slaveConnectionProperties;

    @Bean
    @Override
    public ConnectionFactory connectionFactory() {
        return new DynamicRoutingConnectionFactory(masterConnectionFactory(), slaveConnectionFactory());
    }
      
    @Bean
    public ConnectionFactory masterConnectionFactory() {
        return masterConnectionProperties.createConnectionPool();
    }
      
    @Bean
    public ConnectionFactory slaveConnectionFactory() {
        return slaveConnectionProperties.createConnectionPool();
    }

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory); // (1)
        // return new R2dbcTransactionManager(new DynamicRoutingConnectionFactory(masterConnectionFactory(), slaveConnectionFactory())); // (2)
        // return new R2dbcTransactionManager(new TransactionAwareConnectionFactoryProxy(connectionFactory)); // (3)
    }

    @ConfigurationProperties(prefix = "spring.r2dbc.master")
    public static class MasterConnectionProperties extends ConnectionProperties {
        public MasterConnectionProperties(String driver, String protocol, String host, Integer port, String username,
                                          String password, String database, Duration connectionTimeout, Boolean ssl,
                                          String poolName, Integer initialSize, Integer maxSize, Duration maxIdleTime,
                                          Duration maxLifeTime, Duration maxCreateConnectionTime,
                                          Duration maxAcquireTime, Integer acquireRetry, String validationQuery,
                                          Boolean registerJmx) {
            super(driver, protocol, host, port, username, password, database, connectionTimeout, ssl, poolName,
                  initialSize, maxSize, maxIdleTime, maxLifeTime, maxCreateConnectionTime, maxAcquireTime, acquireRetry,
                  validationQuery, registerJmx);
        }
    }
    
    @ConfigurationProperties(prefix = "spring.r2dbc.slave")
    public static class SlaveConnectionProperties extends ConnectionProperties {
        public SlaveConnectionProperties(String driver, String protocol, String host, Integer port, String username,
                                         String password, String database, Duration connectionTimeout, Boolean ssl,
                                         String poolName, Integer initialSize, Integer maxSize, Duration maxIdleTime,
                                         Duration maxLifeTime, Duration maxCreateConnectionTime,
                                         Duration maxAcquireTime, Integer acquireRetry, String validationQuery,
                                         Boolean registerJmx) {
            super(driver, protocol, host, port, username, password, database, connectionTimeout, ssl, poolName,
                  initialSize, maxSize, maxIdleTime, maxLifeTime, maxCreateConnectionTime, maxAcquireTime, acquireRetry,
                  validationQuery, registerJmx);
        }
    }
}
```
- ReactiveTransactionManager
  - (1) ConnectionFactory 를 주입해서 R2dbcTransactionManager 생성
    - TransactionSynchronizationManager 에서 관리되지 않는다.
    - 트랜잭션 설정에 따라 master/slave 분기 할 수 없고, 항상 master 로 연결된다.
    - 트랜잭션 내에서 동일한 커넥션으로 연결된다.
    - 트랜잭션 내에서 커밋/롤백이 정상 작동한다.
  - (2) 1번과 동일한 동일한 ConnectionPool 을 주입해서 새로운 DynamicRoutingConnectionFactory 객체로 R2dbcTransactionManager 생성
    - TransactionSynchronizationManager 에서 관리된다.
    - transaction 설정에 따라 master/slave 분기 할 수 있다.
    - 트랜잭션 내에서 동일한 커넥션으로 연결된다.
    - `트랜잭션 내에서 예외가 발생하면 롤백이 정상 작동하지 않는다.`
      <details>
      <summary>접기/펼치기</summary>

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

      </details>
      
    - 트랜잭션 내에서 publisher 를 결합(Mono.zip, mono.zipWith, mono.zipWhen, Mono.when ...)하면 예외가 발생한다.
      - org.springframework.r2dbc.connection.ConnectionFactoryUtils 클래스에서 getConnection 을 호출하면 TransactionSynchronizationManager 에서 ConnectionFactory 에 해당하는 ConnectionHolder 를 등록하고 조회하는 부분에서 문제가 생긴다.
      - 하나의 파이프라인에서는 커넥션을 한 번만 가져와서 정상 작동하지만, 여러개의 파이프라인을 결합하는 경우 파이프라인의 수만큼 커넥션을 가져오게 되고 이 부분에 문제가 있는 것으로 보인다. 이부분은 org.springframework.r2dbc.connection.ConnectionFactoryUtils 클래스에서 ConnectionHolder 처리하는 부분을 수정해서 정상 작동하게 할 수 있다.
      - 이 문제는 flatMap 에서 직접 Tuple 을 생성하는 방법으로 우회해서 처리가 가능하다. 하지만 트랜잭션 롤백이 되지 않는다.
      <details>
      <summary>접기/펼치기</summary>
  
      ```java
      public Flux<Line> test() {
          return lineRepository.findAll()
                  .collectList()
                  .zipWith(sectionRepository.findAll())
                  //...
                  .flatMapMany(Flux::fromStream);
      }
      ```

      - 로그에서 "... R2DBC Connection [Transaction-aware proxy for target ..." 라인은 (3)번에서 실행했을 때만 출력되고 (2)번에서는 출력되지 않는다.
      ```
      2022-08-11 16:56:18.785 DEBUG 33935 --- [ctor-http-nio-4] o.s.r.c.R2dbcTransactionManager          : Creating new transaction with name [nextstep.subway.line.application.LineService.findLineResponses]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT,readOnly
      2022-08-11 16:56:18.787  INFO 33935 --- [ctor-http-nio-4] .s.c.c.d.DynamicRoutingConnectionFactory : DynamicRoutingConnectionFactory.determineCurrentLookupKey
      2022-08-11 16:56:18.819 DEBUG 33935 --- [actor-tcp-nio-5] o.s.r.c.R2dbcTransactionManager          : Acquired Connection [MonoMap] for R2DBC transaction
      2022-08-11 16:56:18.819 DEBUG 33935 --- [actor-tcp-nio-5] o.s.r.c.R2dbcTransactionManager          : Switching R2DBC Connection [Transaction-aware proxy for target Connection [PooledConnection[PooledConnection[dev.miku.r2dbc.mysql.MySqlConnection@68724ad7]]]] to manual commit
      2022-08-11 16:56:18.847  INFO 33935 --- [actor-tcp-nio-5] .s.c.c.d.DynamicRoutingConnectionFactory : DynamicRoutingConnectionFactory.determineCurrentLookupKey
      2022-08-11 16:56:18.847 DEBUG 33935 --- [actor-tcp-nio-5] .s.c.c.d.DynamicRoutingConnectionFactory : RoutingConnectionFactory: slave
      2022-08-11 16:56:18.848  INFO 33935 --- [actor-tcp-nio-5] .s.c.c.d.DynamicRoutingConnectionFactory : DynamicRoutingConnectionFactory.determineCurrentLookupKey
      2022-08-11 16:56:18.849 DEBUG 33935 --- [actor-tcp-nio-5] .s.c.c.d.DynamicRoutingConnectionFactory : RoutingConnectionFactory: slave
      2022-08-11 16:56:18.849 DEBUG 33935 --- [actor-tcp-nio-5] o.s.r.c.R2dbcTransactionManager          : Participating in existing transaction
      2022-08-11 16:56:18.849  INFO 33935 --- [actor-tcp-nio-5] .s.c.c.d.DynamicRoutingConnectionFactory : DynamicRoutingConnectionFactory.determineCurrentLookupKey
      2022-08-11 16:56:18.849 DEBUG 33935 --- [actor-tcp-nio-5] .s.c.c.d.DynamicRoutingConnectionFactory : RoutingConnectionFactory: slave
      2022-08-11 16:56:18.856 DEBUG 33935 --- [actor-tcp-nio-5] o.s.r2dbc.core.DefaultDatabaseClient     : Executing SQL statement [SELECT station.* FROM station]
      2022-08-11 16:56:18.860 DEBUG 33935 --- [actor-tcp-nio-5] o.s.r.c.R2dbcTransactionManager          : Participating transaction failed - marking existing transaction as rollback-only
      2022-08-11 16:56:18.861 DEBUG 33935 --- [actor-tcp-nio-5] o.s.r.c.R2dbcTransactionManager          : Setting R2DBC transaction [Transaction-aware proxy for target Connection [PooledConnection[PooledConnection[dev.miku.r2dbc.mysql.MySqlConnection@68724ad7]]]] rollback-only
      2022-08-11 16:56:18.861 DEBUG 33935 --- [actor-tcp-nio-5] o.s.r.c.R2dbcTransactionManager          : Initiating transaction rollback
      2022-08-11 16:56:18.861 DEBUG 33935 --- [actor-tcp-nio-5] o.s.r.c.R2dbcTransactionManager          : Rolling back R2DBC transaction on Connection [Transaction-aware proxy for target Connection [PooledConnection[PooledConnection[dev.miku.r2dbc.mysql.MySqlConnection@68724ad7]]]]
      2022-08-11 16:56:18.881 DEBUG 33935 --- [actor-tcp-nio-5] o.s.r.c.R2dbcTransactionManager          : Releasing R2DBC Connection [Transaction-aware proxy for target Connection [PooledConnection[PooledConnection[dev.miku.r2dbc.mysql.MySqlConnection@68724ad7]]]] after transaction
      2022-08-11 16:56:18.885 ERROR 33935 --- [actor-tcp-nio-5] c.a.GlobalRestControllerExceptionHandler : handle RuntimeException

      org.springframework.dao.DataAccessResourceFailureException: Failed to obtain R2DBC Connection; nested exception is java.lang.IllegalStateException: Already value [org.springframework.r2dbc.connection.ConnectionHolder@4de25099] for key [nextstep.subway.common.config.datasource.DynamicRoutingConnectionFactory@307a4c18] bound to context
      at org.springframework.r2dbc.connection.ConnectionFactoryUtils.lambda$getConnection$0(ConnectionFactoryUtils.java:88)
      Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
      Error has been observed at the following site(s):
      *__checkpoint ⇢ Handler nextstep.subway.line.ui.LineController#findAllLines() [DispatcherHandler]
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
      at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
      at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.onComplete(Operators.java:2058)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:209)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:209)
      at reactor.pool.SimpleDequePool.maybeRecycleAndDrain(SimpleDequePool.java:533)
      at reactor.pool.SimpleDequePool$QueuePoolRecyclerInner.onComplete(SimpleDequePool.java:765)
      at reactor.core.publisher.Operators.complete(Operators.java:137)
      at reactor.core.publisher.MonoEmpty.subscribe(MonoEmpty.java:46)
      at reactor.core.publisher.Mono.subscribe(Mono.java:4397)
      at reactor.pool.SimpleDequePool$QueuePoolRecyclerMono.subscribe(SimpleDequePool.java:877)
      at reactor.core.publisher.MonoDefer.subscribe(MonoDefer.java:52)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:240)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:203)
      at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
      at reactor.core.publisher.Operators.complete(Operators.java:137)
      at reactor.core.publisher.MonoEmpty.subscribe(MonoEmpty.java:46)
      at reactor.core.publisher.Mono.subscribe(Mono.java:4397)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:263)
      at reactor.core.publisher.MonoIgnoreThen.subscribe(MonoIgnoreThen.java:51)
      at reactor.core.publisher.MonoDefer.subscribe(MonoDefer.java:52)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:240)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:203)
      at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
      at reactor.core.publisher.FluxHandleFuseable$HandleFuseableSubscriber.onComplete(FluxHandleFuseable.java:236)
      at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1817)
      at reactor.core.publisher.MonoSupplier.subscribe(MonoSupplier.java:62)
      at reactor.core.publisher.Mono.subscribe(Mono.java:4397)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:263)
      at reactor.core.publisher.MonoIgnoreThen.subscribe(MonoIgnoreThen.java:51)
      at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:64)
      at reactor.core.publisher.MonoDefer.subscribe(MonoDefer.java:52)
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
      at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1816)
      at reactor.core.publisher.MonoFlatMap$FlatMapInner.onNext(MonoFlatMap.java:249)
      at reactor.core.publisher.FluxRetry$RetrySubscriber.onNext(FluxRetry.java:87)
      at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onNext(FluxOnErrorResume.java:79)
      at reactor.core.publisher.SerializedSubscriber.onNext(SerializedSubscriber.java:99)
      at reactor.core.publisher.SerializedSubscriber.onNext(SerializedSubscriber.java:99)
      at reactor.core.publisher.FluxTimeout$TimeoutMainSubscriber.onNext(FluxTimeout.java:180)
      at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1816)
      at reactor.core.publisher.MonoFlatMap$FlatMapInner.onNext(MonoFlatMap.java:249)
      at io.r2dbc.pool.MonoDiscardOnCancel$MonoDiscardOnCancelSubscriber.onNext(MonoDiscardOnCancel.java:92)
      at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onNext(FluxOnErrorResume.java:79)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.complete(MonoIgnoreThen.java:292)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onNext(MonoIgnoreThen.java:187)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:236)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:203)
      at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.onComplete(Operators.java:2058)
      at reactor.core.publisher.SerializedSubscriber.onComplete(SerializedSubscriber.java:146)
      at reactor.core.publisher.SerializedSubscriber.onComplete(SerializedSubscriber.java:146)
      at reactor.core.publisher.FluxTimeout$TimeoutMainSubscriber.onComplete(FluxTimeout.java:234)
      at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
      at reactor.core.publisher.FluxFlatMap$FlatMapMain.checkTerminated(FluxFlatMap.java:846)
      at reactor.core.publisher.FluxFlatMap$FlatMapMain.drainLoop(FluxFlatMap.java:608)
      at reactor.core.publisher.FluxFlatMap$FlatMapMain.drain(FluxFlatMap.java:588)
      at reactor.core.publisher.FluxFlatMap$FlatMapMain.onComplete(FluxFlatMap.java:465)
      at reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)
      at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.checkTerminated(FluxWindowPredicate.java:538)
      at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.drainLoop(FluxWindowPredicate.java:486)
      at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.drain(FluxWindowPredicate.java:430)
      at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.onComplete(FluxWindowPredicate.java:310)
      at reactor.core.publisher.FluxHandleFuseable$HandleFuseableSubscriber.onComplete(FluxHandleFuseable.java:236)
      at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onComplete(FluxContextWrite.java:126)
      at dev.miku.r2dbc.mysql.util.DiscardOnCancelSubscriber.onComplete(DiscardOnCancelSubscriber.java:104)
      at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onComplete(FluxPeekFuseable.java:940)
      at reactor.core.publisher.MonoFlatMapMany$FlatMapManyInner.onComplete(MonoFlatMapMany.java:260)
      at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
      at reactor.core.publisher.FluxHandle$HandleSubscriber.onComplete(FluxHandle.java:220)
      at reactor.core.publisher.FluxHandle$HandleSubscriber.onNext(FluxHandle.java:141)
      at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onNext(FluxPeekFuseable.java:854)
      at reactor.core.publisher.EmitterProcessor.drain(EmitterProcessor.java:537)
      at reactor.core.publisher.EmitterProcessor.tryEmitNext(EmitterProcessor.java:343)
      at reactor.core.publisher.InternalManySink.emitNext(InternalManySink.java:27)
      at reactor.core.publisher.EmitterProcessor.onNext(EmitterProcessor.java:309)
      at dev.miku.r2dbc.mysql.client.ReactorNettyClient$ResponseSink.next(ReactorNettyClient.java:340)
      at dev.miku.r2dbc.mysql.client.ReactorNettyClient.lambda$new$0(ReactorNettyClient.java:103)
      at reactor.core.publisher.FluxPeek$PeekSubscriber.onNext(FluxPeek.java:185)
      at reactor.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:279)
      at reactor.netty.channel.FluxReceive.onInboundNext(FluxReceive.java:388)
      at reactor.netty.channel.ChannelOperations.onInboundNext(ChannelOperations.java:404)
      at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:93)
      at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
      at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
      at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
      at dev.miku.r2dbc.mysql.client.MessageDuplexCodec.handleDecoded(MessageDuplexCodec.java:187)
      at dev.miku.r2dbc.mysql.client.MessageDuplexCodec.channelRead(MessageDuplexCodec.java:95)
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
      Caused by: java.lang.IllegalStateException: Already value [org.springframework.r2dbc.connection.ConnectionHolder@4de25099] for key [nextstep.subway.common.config.datasource.DynamicRoutingConnectionFactory@307a4c18] bound to context
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
      at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1816)
      at reactor.core.publisher.MonoFlatMap$FlatMapInner.onNext(MonoFlatMap.java:249)
      at reactor.core.publisher.FluxRetry$RetrySubscriber.onNext(FluxRetry.java:87)
      at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onNext(FluxOnErrorResume.java:79)
      at reactor.core.publisher.SerializedSubscriber.onNext(SerializedSubscriber.java:99)
      at reactor.core.publisher.SerializedSubscriber.onNext(SerializedSubscriber.java:99)
      at reactor.core.publisher.FluxTimeout$TimeoutMainSubscriber.onNext(FluxTimeout.java:180)
      at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1816)
      at reactor.core.publisher.MonoFlatMap$FlatMapInner.onNext(MonoFlatMap.java:249)
      at io.r2dbc.pool.MonoDiscardOnCancel$MonoDiscardOnCancelSubscriber.onNext(MonoDiscardOnCancel.java:92)
      at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onNext(FluxOnErrorResume.java:79)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.complete(MonoIgnoreThen.java:292)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onNext(MonoIgnoreThen.java:187)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:236)
      at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:203)
      at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.onComplete(Operators.java:2058)
      at reactor.core.publisher.SerializedSubscriber.onComplete(SerializedSubscriber.java:146)
      at reactor.core.publisher.SerializedSubscriber.onComplete(SerializedSubscriber.java:146)
      at reactor.core.publisher.FluxTimeout$TimeoutMainSubscriber.onComplete(FluxTimeout.java:234)
      at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
      at reactor.core.publisher.FluxFlatMap$FlatMapMain.checkTerminated(FluxFlatMap.java:846)
      at reactor.core.publisher.FluxFlatMap$FlatMapMain.drainLoop(FluxFlatMap.java:608)
      at reactor.core.publisher.FluxFlatMap$FlatMapMain.drain(FluxFlatMap.java:588)
      at reactor.core.publisher.FluxFlatMap$FlatMapMain.onComplete(FluxFlatMap.java:465)
      at reactor.core.publisher.FluxMap$MapSubscriber.onComplete(FluxMap.java:144)
      at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.checkTerminated(FluxWindowPredicate.java:538)
      at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.drainLoop(FluxWindowPredicate.java:486)
      at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.drain(FluxWindowPredicate.java:430)
      at reactor.core.publisher.FluxWindowPredicate$WindowPredicateMain.onComplete(FluxWindowPredicate.java:310)
      at reactor.core.publisher.FluxHandleFuseable$HandleFuseableSubscriber.onComplete(FluxHandleFuseable.java:236)
      at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onComplete(FluxContextWrite.java:126)
      at dev.miku.r2dbc.mysql.util.DiscardOnCancelSubscriber.onComplete(DiscardOnCancelSubscriber.java:104)
      at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onComplete(FluxPeekFuseable.java:940)
      at reactor.core.publisher.MonoFlatMapMany$FlatMapManyInner.onComplete(MonoFlatMapMany.java:260)
      at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
      at reactor.core.publisher.FluxHandle$HandleSubscriber.onComplete(FluxHandle.java:220)
      at reactor.core.publisher.FluxHandle$HandleSubscriber.onNext(FluxHandle.java:141)
      at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onNext(FluxPeekFuseable.java:854)
      at reactor.core.publisher.EmitterProcessor.drain(EmitterProcessor.java:537)
      at reactor.core.publisher.EmitterProcessor.tryEmitNext(EmitterProcessor.java:343)
      at reactor.core.publisher.InternalManySink.emitNext(InternalManySink.java:27)
      at reactor.core.publisher.EmitterProcessor.onNext(EmitterProcessor.java:309)
      at dev.miku.r2dbc.mysql.client.ReactorNettyClient$ResponseSink.next(ReactorNettyClient.java:340)
      at dev.miku.r2dbc.mysql.client.ReactorNettyClient.lambda$new$0(ReactorNettyClient.java:103)
      at reactor.core.publisher.FluxPeek$PeekSubscriber.onNext(FluxPeek.java:185)
      at reactor.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:279)
      at reactor.netty.channel.FluxReceive.onInboundNext(FluxReceive.java:388)
      at reactor.netty.channel.ChannelOperations.onInboundNext(ChannelOperations.java:404)
      at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:93)
      at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
      at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
      at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
      at dev.miku.r2dbc.mysql.client.MessageDuplexCodec.handleDecoded(MessageDuplexCodec.java:187)
      at dev.miku.r2dbc.mysql.client.MessageDuplexCodec.channelRead(MessageDuplexCodec.java:95)
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
  - (3) TransactionAwareConnectionFactoryProxy 는 ConnectionFactory 를 감싸면 r2dbc client 가 스프링이 관리하는 트랜잭션에 참여하도록 만들어준다.
    - 2번과 동일한 결과를 보인다.
  - TransactionManager 를 master/slave 각각 생성해서 @Transactional(transactionManager = "masterTransactionManager") 의 방법도 예외가 발생한다.
    <details>
    <summary>접기/펼치기</summary>

    ```java
    @Configuration
    @EnableTransactionManagement
    public class ConnectionFactoryConfig extends AbstractR2dbcConfiguration {
        // ...

        @Bean(name = "masterTransactionManager")
        public ReactiveTransactionManager masterTransactionManager() {
            return new R2dbcTransactionManager(masterConnectionFactory());
        }

        @Bean(name = "slaveTransactionManager")
        public ReactiveTransactionManager slaveTransactionManager() {
            R2dbcTransactionManager r2dbcTransactionManager = new R2dbcTransactionManager(slaveConnectionFactory());
            r2dbcTransactionManager.setEnforceReadOnly(true);
            return r2dbcTransactionManager;
        }

        // ...
    }
    ```
    ```
    org.springframework.transaction.CannotCreateTransactionException: Could not open R2DBC Connection for transaction; nested exception is io.r2dbc.spi.R2dbcNonTransientResourceException: [1568] Transaction characteristics can't be changed while a transaction is in progress
    	at org.springframework.r2dbc.connection.R2dbcTransactionManager.lambda$null$5(R2dbcTransactionManager.java:227)
    	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
    Error has been observed at the following site(s):
        *__checkpoint ⇢ Handler nextstep.subway.line.ui.LineController#findAllLines() [DispatcherHandler]
    Original Stack Trace:
            at org.springframework.r2dbc.connection.R2dbcTransactionManager.lambda$null$5(R2dbcTransactionManager.java:227)
            at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:94)
            at reactor.core.publisher.MonoFlatMap$FlatMapMain.secondError(MonoFlatMap.java:192)
            at reactor.core.publisher.MonoFlatMap$FlatMapInner.onError(MonoFlatMap.java:259)
            at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:106)
            at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onError(MonoIgnoreThen.java:278)
            at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:231)
            at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:203)
            at reactor.core.publisher.MonoPeekTerminal$MonoTerminalPeekSubscriber.onComplete(MonoPeekTerminal.java:299)
            at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.onComplete(Operators.java:2058)
            at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.onComplete(Operators.java:2058)
            at reactor.core.publisher.MonoFlatMap$FlatMapMain.secondComplete(MonoFlatMap.java:196)
            at reactor.core.publisher.MonoFlatMap$FlatMapInner.onComplete(MonoFlatMap.java:268)
            at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
            at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.onComplete(Operators.java:2058)
            at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:209)
            at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:209)
            at reactor.pool.SimpleDequePool.maybeRecycleAndDrain(SimpleDequePool.java:533)
            at reactor.pool.SimpleDequePool$QueuePoolRecyclerInner.onComplete(SimpleDequePool.java:765)
            at reactor.core.publisher.Operators.complete(Operators.java:137)
            at reactor.core.publisher.MonoEmpty.subscribe(MonoEmpty.java:46)
            at reactor.core.publisher.Mono.subscribe(Mono.java:4397)
            at reactor.pool.SimpleDequePool$QueuePoolRecyclerMono.subscribe(SimpleDequePool.java:877)
            at reactor.core.publisher.MonoDefer.subscribe(MonoDefer.java:52)
            at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.subscribeNext(MonoIgnoreThen.java:240)
            at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.onComplete(MonoIgnoreThen.java:203)
            at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
            at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.onComplete(Operators.java:2058)
            at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
            at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
            at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
            at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onComplete(FluxPeekFuseable.java:277)
            at reactor.core.publisher.FluxHandleFuseable$HandleFuseableSubscriber.onComplete(FluxHandleFuseable.java:236)
            at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onComplete(FluxContextWrite.java:126)
            at dev.miku.r2dbc.mysql.util.DiscardOnCancelSubscriber.onComplete(DiscardOnCancelSubscriber.java:104)
            at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onComplete(FluxPeekFuseable.java:940)
            at reactor.core.publisher.MonoFlatMapMany$FlatMapManyInner.onComplete(MonoFlatMapMany.java:260)
            at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
            at reactor.core.publisher.FluxHandle$HandleSubscriber.onComplete(FluxHandle.java:220)
            at reactor.core.publisher.FluxHandle$HandleSubscriber.onNext(FluxHandle.java:141)
            at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onNext(FluxPeekFuseable.java:854)
            at reactor.core.publisher.EmitterProcessor.drain(EmitterProcessor.java:537)
            at reactor.core.publisher.EmitterProcessor.tryEmitNext(EmitterProcessor.java:343)
            at reactor.core.publisher.InternalManySink.emitNext(InternalManySink.java:27)
            at reactor.core.publisher.EmitterProcessor.onNext(EmitterProcessor.java:309)
            at dev.miku.r2dbc.mysql.client.ReactorNettyClient$ResponseSink.next(ReactorNettyClient.java:340)
            at dev.miku.r2dbc.mysql.client.ReactorNettyClient.lambda$new$0(ReactorNettyClient.java:103)
            at reactor.core.publisher.FluxPeek$PeekSubscriber.onNext(FluxPeek.java:185)
            at reactor.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:279)
            at reactor.netty.channel.FluxReceive.onInboundNext(FluxReceive.java:388)
            at reactor.netty.channel.ChannelOperations.onInboundNext(ChannelOperations.java:404)
            at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:93)
            at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
            at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
            at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
            at dev.miku.r2dbc.mysql.client.MessageDuplexCodec.handleDecoded(MessageDuplexCodec.java:187)
            at dev.miku.r2dbc.mysql.client.MessageDuplexCodec.channelRead(MessageDuplexCodec.java:95)
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
    Caused by: io.r2dbc.spi.R2dbcNonTransientResourceException: Transaction characteristics can't be changed while a transaction is in progress
        at dev.miku.r2dbc.mysql.ExceptionFactory.mappingSqlState(ExceptionFactory.java:115)
        at dev.miku.r2dbc.mysql.ExceptionFactory.createException(ExceptionFactory.java:102)
        at dev.miku.r2dbc.mysql.QueryFlow.lambda$execute0$8(QueryFlow.java:241)
        at reactor.core.publisher.FluxHandleFuseable$HandleFuseableSubscriber.onNext(FluxHandleFuseable.java:176)
        at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onNext(FluxContextWrite.java:107)
        at dev.miku.r2dbc.mysql.util.DiscardOnCancelSubscriber.onNext(DiscardOnCancelSubscriber.java:70)
        at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onNext(FluxPeekFuseable.java:854)
        at reactor.core.publisher.MonoFlatMapMany$FlatMapManyInner.onNext(MonoFlatMapMany.java:250)
        at reactor.core.publisher.FluxPeek$PeekSubscriber.onNext(FluxPeek.java:200)
        at reactor.core.publisher.FluxHandle$HandleSubscriber.onNext(FluxHandle.java:126)
        at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onNext(FluxPeekFuseable.java:854)
        at reactor.core.publisher.EmitterProcessor.drain(EmitterProcessor.java:537)
        at reactor.core.publisher.EmitterProcessor.tryEmitNext(EmitterProcessor.java:343)
        at reactor.core.publisher.InternalManySink.emitNext(InternalManySink.java:27)
        at reactor.core.publisher.EmitterProcessor.onNext(EmitterProcessor.java:309)
        at dev.miku.r2dbc.mysql.client.ReactorNettyClient$ResponseSink.next(ReactorNettyClient.java:340)
        at dev.miku.r2dbc.mysql.client.ReactorNettyClient.lambda$new$0(ReactorNettyClient.java:103)
        at reactor.core.publisher.FluxPeek$PeekSubscriber.onNext(FluxPeek.java:185)
        at reactor.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:279)
        at reactor.netty.channel.FluxReceive.onInboundNext(FluxReceive.java:388)
        at reactor.netty.channel.ChannelOperations.onInboundNext(ChannelOperations.java:404)
        at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:93)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
        at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
        at dev.miku.r2dbc.mysql.client.MessageDuplexCodec.handleDecoded(MessageDuplexCodec.java:187)
        at dev.miku.r2dbc.mysql.client.MessageDuplexCodec.channelRead(MessageDuplexCodec.java:95)
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

- 위와 같이 트랜잭션 설정에 따라 데이터베이스 연결을 분기하면 트랜잭션이 정상 작동하지 않아서 master 만 연결하게 했다.
  - ReactiveTransactionManager 를 빈으로 등록하지 않아도 @Transactional 은 정상적으로 동작한다.
  - @EnableTransactionManagement 설정을 하지 않아도 @Transactional 은 정상적으로 동작한다.
  ```java
  @Profile({"local", "prod"})
  @Configuration
  @ConfigurationPropertiesScan
  @EnableConfigurationProperties
  @RequiredArgsConstructor
  public class ConnectionFactoryConfig extends AbstractR2dbcConfiguration {
      private final MasterConnectionProperties masterConnectionProperties;
  
      @Bean
      @Override
      public ConnectionFactory connectionFactory() {
          return masterConnectionProperties.createConnectionPool();
      }
  
      @ConfigurationProperties(prefix = "spring.r2dbc")
      public static class MasterConnectionProperties extends ConnectionProperties {
          public MasterConnectionProperties(String driver, String protocol, String host, Integer port, String username,
                                            String password, String database, Duration connectionTimeout, Boolean ssl,
                                            String poolName, Integer initialSize, Integer maxSize, Duration maxIdleTime,
                                            Duration maxLifeTime, Duration maxCreateConnectionTime,
                                            Duration maxAcquireTime, Integer acquireRetry, String validationQuery,
                                            Boolean registerJmx) {
              super(driver, protocol, host, port, username, password, database, connectionTimeout, ssl, poolName,
                    initialSize, maxSize, maxIdleTime, maxLifeTime, maxCreateConnectionTime, maxAcquireTime, acquireRetry,
                    validationQuery, registerJmx);
          }
      }
  }
  ```
