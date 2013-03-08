(ns ring.middleware.ratelimit
  (:use [ring.middleware.ratelimit util backend local-atom limits]))

(defn ip-limit [n] (-> n limit wrap-limit-ip))

(defn user-limit [n] (-> n limit wrap-limit-user))

(defn role-limit [role n] (-> n limit (wrap-limit-role role)))

(defn default-config []
  {:limits [(ip-limit 100)]
   :backend (local-atom-backend)
   :err-handler (fn [req]
                  {:status 429
                   :headers {"Content-Type" "application/json"}
                   :body "{\"error\": \"Too Many Requests\"}"})})

(defn wrap-ratelimit
  ([handler] (wrap-ratelimit handler {}))
  ([handler config]
   (let [config* (merge (default-config) config)
         limits (:limits config*)
         backend (:backend config*)
         err-handler (:err-handler config*)]
     (fn [req]
       (if (available? backend)
         (do
           (when (not= (get-hour backend) (current-hour))
             (reset-limits! backend (current-hour)))
           (let [limiter (first (filter #((:filter %) req) limits))
                 limit (:limit limiter)
                 thekey (str (:key-prefix limiter) ((:getter limiter) req))
                 current (get-limit backend limit thekey)
                 rl-headers {"X-RateLimit-Limit" (str limit)
                             "X-RateLimit-Remaining" (str (- limit current))}
                 h (if (< current limit) handler err-handler)
                 rsp (h req)]
             (assoc rsp :headers (merge (:headers rsp) rl-headers))))
         (handler req))))))
