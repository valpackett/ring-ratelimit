(ns ring.middleware.ratelimit.redis
  (:require [taoensso.carmine :as car])
  (:use [ring.middleware.ratelimit backend]))

(deftype RedisBackend [pool spec hashname] Backend
  (get-limit [self limit k]
    (min limit (car/with-conn pool spec (car/hincrby hashname k 1))))
  (reset-limits! [self]
    (car/with-conn pool spec (car/del hashname))))

(defn redis-backend
  ([] (redis-backend (car/make-conn-spec)))
  ([spec] (redis-backend (car/make-conn-pool) spec))
  ([pool spec] (redis-backend pool spec "ratelimits"))
  ([pool spec hashname] (RedisBackend. pool spec hashname)))
