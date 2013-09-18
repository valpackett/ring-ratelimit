(ns ring.middleware.ratelimit.redis
  (:require [taoensso.carmine :as car]
            [taoensso.carmine.connections :as conns])
  (:use [ring.middleware.ratelimit backend]))

(deftype RedisBackend [pool spec hashname hourname] Backend
  (get-limit [self limit k]
    (car/with-conn pool spec (car/hincrby hashname k 1)))
  (reset-limits! [self hour]
    (car/with-conn pool spec
      (car/del hashname)
      (car/set hourname hour)))
  (get-hour [self]
    (Integer/parseInt (car/with-conn pool spec (car/get hourname))))
  (available? [self]
    (= "PONG" (try (car/with-conn pool spec (car/ping))
                (catch Throwable _)))))

(defn redis-backend
  ([] (redis-backend (car/make-conn-spec)))
  ([spec] (redis-backend (car/make-conn-pool) spec))
  ([pool spec] (redis-backend pool spec "ratelimits"))
  ([pool spec hashname] (RedisBackend. pool spec hashname (str hashname ":hour"))))
