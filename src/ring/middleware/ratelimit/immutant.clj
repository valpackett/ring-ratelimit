(ns ring.middleware.ratelimit.immutant
  (:use [ring.middleware.ratelimit backend]
        [immutant cache xa]))

(deftype ImmutantBackend [obj] Backend
  (get-limit [self limit k]
    (transaction
      (if-let [current (get obj k)]
        (let [newl (inc current)]
          (put obj k newl)
          newl)
        (do
          (put obj k 1)
          1))))
  (reset-limits! [self hour]
    (delete-all obj)
    (put obj :hour hour))
  (get-hour [self] (:hour obj))
  (available? [self] true))

(defn immutant-backend
  ([] (immutant-backend (cache "ratelimit")))
  ([obj] (ImmutantBackend. obj)))
