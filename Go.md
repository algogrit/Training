# Course structure offered

- (Idiomatic) Go for Java developers
- Go for Ruby/Python developers
- Go for novices

## Outline

1. Setting up a Go environment (1/2 day)
    - Setting up Visual Studio Code (alternatively setting up vim)
    - Getting delve debugger running (optional)
    - Setting up `dep` package manager (brief intro to `go mod` tool, which is still experimental)
        - `go get -u github.com/golang/dep/cmd/dep`
        - Comparing with Maven (for Java)
        - Comparing with Bundle (for Ruby)
        - Comparing with Pip (for Python)
2. Syntax overview of Go programming language by writing your first `Hello, world!` API (1 day)
    - Introduction to closures (for Java)
    - Overview of Go standard library
        - fmt
        - io
        - os
        - net/http
        - encoding/json
3. Using Go in production and everyday development (1 day)
    - Overview of third-party packages in Go
        - gorilla/mux (Routing)
        - negroni (Middleware)
        - logrus (Logging)
        - gorm/sqlx (Database)
        - crypto/bcrypt (Cryptography)
        - etc. (This depends on the audience and what they are looking for)
    - Deploying and packaging a Go application
        - `go build`
        - `context.Context`
        - Structuring a Go application
        - Writing a multi-step `docker` file to package the app
        - Other small Tidbits (Teaser: https://www.youtube.com/watch?v=W95NLFtCMN0)
    - Debugging
    - Testing a Go API
        - xUnit style tests using built-in `testing` package (for Java)
        - BDD style tests using `ginkgo` & `gomega` (for Ruby)
        - Overview of `net/http/httptest`
    - Benchmarking and profiling
        - https://blog.golang.org/profiling-go-programs
        - https://jvns.ca/blog/2017/09/24/profiling-go-with-pprof/
        - https://www.youtube.com/watch?v=YNye3SZWvj8
        - Run one profiler at a time, CPU profiler takes measurements every 10ms
        - `go test -bench=.`
        - `go test -cpuprofile cpu.prof -memprofile mem.prof -bench .`
        - `go tool pprof cpu.prof`
        - `go tool pprof --alloc_objects mep.prof`
        - `go tool pprof --inuse_objects mep.prof`
        - `import _ "net/http/pprof"`
        - `go tool pprof -seconds 10 http://localhost:8080/debug/pprof/profile`
            - `wrk -t 4 -c 16 -d 30 http://localhost:8000/posts`
        - `go get -u github.com/google/pprof`
            - `pprof cpu.prof`
            - `pprof -seconds 10 -http=localhost:8181 http://localhost:8080/debug/pprof/profile`
4. Working with Go in a distributed environment (3 days)
    - Go concurrency constructs (Teaser: https://www.youtube.com/watch?v=JqNpNpb5TlQ)
        - Go routines
        - sync.WaitGroup
        - Mutexes
        - Channels
        - Select
    - Overview of 3rd party distributed libraries
        - uber-go/zap for logging
        - Jaeger for tracing (https://blog.golang.org/http-tracing)
        - Hystrix for Latency & Fault tolerance
    - Go with Kubernetes
    - Go in a serverless environment

Each section of `Go in a distributed environment` would take at least a day each to cover in depth.
