# ring-ratelimit

Ring middleware for request rate limiting.

## Usage

```clojure
(ns your.app
  (:use [ring.middleware ratelimit]))

(def app (-> your-routes-or-whatever
             (wrap-ratelimit {:limits [(ip-limit 100)]})))
; 100 per hour per IP address
```

If you're using [friend](https://github.com/cemerick/friend) authentication, you can use `user-limit` and `role-limit`:

```clojure
; ...

(def app (-> your-routes-or-whatever
             (wrap-ratelimit {:limits [(role-limit :admin 1000)
                                       (user-limit 500)
                                       (ip-limit 100)]})
             (friend/authenticate ; ...
             )))
; 1000 per hour per user for authenticated users with role :admin
; 500 per hour per user for all other authenticated users
; 100 per hour per IP address for anonymous users
```

You can use a custom handler for the error (when the user has no requests left):

```clojure
; ...

(defn err-handler [req]
  {:status 429
   :headers {"Content-Type" "text/html"}
   :body (your-error-template {:text "Too many requests"})})

; ...wrapping thing skipped...
(wrap-ratelimit {:limits [(ip-limit 100)]
                 :err-handler err-handler})
```

If you're running your app on multiple JVM instances (eg. multiple Heroku dynos), the default backend (local atom) is not enough for you.
ring-ratelimit supports Redis via the [Carmine](https://github.com/ptaoussanis/carmine) library.
**Don't forget to add it to your dependencies!**

```clojure
(ns your.app
  (:use [ring.middleware ratelimit]
        [ring.middleware.ratelimit redis]))
; ...

; ...wrapping thing skipped...
(wrap-ratelimit {:limits [(ip-limit 100)]
                 :backend (redis-backend pool spec)})
```

The `redis-backend` function, when called with no args, calls `make-conn-pool` and `make-conn-spec` with no args (ie. uses Redis on localhost) and uses `ratelimits` for Redis hash name.
You can provide the Carmine configuration objects (pool, spec) and the hash name as args.

## License

           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                   Version 2, December 2004

Copyright (C) 2013 Greg V

Everyone is permitted to copy and distribute verbatim or modified
copies of this license document, and changing it is allowed as long
as the name is changed.

           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
  TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

 0. You just DO WHAT THE FUCK YOU WANT TO.
