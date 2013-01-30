(ns ring.middleware.ratelimit.util
  (:import [java.util Date]))

(defn current-hour []
  (.getHours (Date.)))
