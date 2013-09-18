(ns ring.middleware.ratelimit.local-atom
  (:use [ring.middleware.ratelimit util backend]))

(defn- update-state [state limit k]
  (assoc state k
    (if-let [v (state k)] (inc v) 1)))

(deftype LocalAtomBackend [rate-map hour-atom] Backend
  (get-limit [self limit k]
    ((swap! rate-map update-state limit k) k))
  (reset-limits! [self hour]
    (reset! rate-map {})
    (reset! hour-atom hour))
  (get-hour [self] @hour-atom)
  (available? [self] true))

(def ^:private default-rate-map (atom {}))
(def ^:private default-hour (atom (current-hour)))

(defn local-atom-backend
  ([] (local-atom-backend default-rate-map))
  ([rate-map] (local-atom-backend rate-map default-hour))
  ([rate-map hour-atom] (LocalAtomBackend. rate-map hour-atom)))
