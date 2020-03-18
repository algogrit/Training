# Spark

> A [micro framework](http://sparkjava.com/) for creating web applications in Kotlin and Java 8 with minimal effort.

## Outline

### Basics

#### Prep and setup

- Using maven/gradle to create a new project
  - Adding spark dependency
- Choosing Kotlin or Java to write our first "Hello, World!" application

#### Introduction to RESTful API

- Routing in Spark
- Path groups
- Request object
- Response object

#### Additional objects

- Query maps
- Cookies
- Sessions

#### Middleware using filters

- Before
- After
- Halts

#### Errors & Exceptions

- Error handling & propagation
- Exception Mapping

#### Views and Templates

- Choosing a templating engine
- Mapping models to views

### Advanced

- Thread pooling for optimized concurrency
- Using websockets
- GZIP
- Using SSL

- Spark for building micro service
  - Domain-driven design
    - Understanding CQRS pattern
  - Choosing an ORM layer
    - Exploring [ebean](https://ebean.io/)
    - Exploring [JOOQ](http://www.jooq.org/)
  - Structuring the app into an MVC pattern
  - Putting it all together to build a RESTful API

- Using spark as a static server
- Logging
