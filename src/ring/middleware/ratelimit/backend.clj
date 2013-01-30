(ns ring.middleware.ratelimit.backend)

(defprotocol Backend
  (get-limit [self limit k])
  (reset-limits! [self hour])
  (get-hour [self]))
