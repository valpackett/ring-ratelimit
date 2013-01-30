(ns ring.middleware.ratelimit
  (:use [ring.middleware.ratelimit util backend local-atom]))

(def default-config
  {:limit 100
   :backend (local-atom-backend)
   :err-handler (fn [req]
                  {:status 429
                   :headers {"Content-Type" "application/json"}
                   :body "{\"error\": \"Too Many Requests\"}"})})

(defn wrap-ratelimit
  ([handler] (wrap-ratelimit handler {}))
  ([handler config]
   (let [config* (merge default-config config)
         limit (:limit config*)
         backend (:backend config*)
         err-handler (:err-handler config*)]
     (fn [req]
       (when (not= (get-hour backend) (current-hour))
         (reset-limits! backend (current-hour)))
       (let [ip (:remote-addr req)
             current (get-limit backend limit ip)
             rl-headers {"X-RateLimit-Limit" (str limit)
                         "X-RateLimit-Remaining" (str (- limit current))}
             h (if (< current limit) handler err-handler)
             rsp (h req)]
         (assoc rsp :headers (merge (:headers rsp) rl-headers)))))))
