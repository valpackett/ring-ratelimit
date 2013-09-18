(ns ring.middleware.ratelimit-test
  (:import [java.util Date])
  (:require [clojure.string :as string]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]
                             [openid :as openid]))
  (:use [midje sweet]
        [ring.middleware ratelimit]
        [ring.middleware.ratelimit util backend local-atom redis limits]
        [ring.mock request]))

(defn rsp-limit [rsp] (get-in rsp [:headers "X-RateLimit-Limit"]))
(defn remaining [rsp] (get-in rsp [:headers "X-RateLimit-Remaining"]))

(doseq [backend [(local-atom-backend) (redis-backend)]]
  (let [app (-> (fn [req] {:status 418
                           :headers {"Content-Type" "air/plane"}
                           :body "Hello"})
                (wrap-ratelimit {:limits [(ip-limit 5)]
                                 :backend backend}))]

    (facts (str "ratelimit <" (last (string/split (str (type backend)) #"\.")) ">")

      (reset-limits! backend (current-hour))

      (fact "shows the rate limit"
        (let [rsp (-> (request :get "/") app)]
          (:status rsp) => 418
          (rsp-limit rsp) => "5"
          (remaining rsp) => "4"))

      (fact "returns 429 when there are no requests left"
        (dotimes [_ 5] (-> (request :get "/") app))
        (let [rsp (-> (request :get "/") app)]
          (:status rsp) => 429
          (remaining rsp) => "0"))

      (fact "resets the limit every hour"
        (with-redefs [current-hour (fn [] (- (.getHours (Date.)) 1))]
          (dotimes [_ 5] (-> (request :get "/") app))
          (-> (request :get "/") app :status) => 429)
        (-> (request :get "/") app :status) => 418))))

(let [api-users {"api-key" {:username "api-key"
                            :password (creds/hash-bcrypt "api-pass")
                            :roles #{:api}}
                 "admin" {:username "admin"
                          :password (creds/hash-bcrypt "admin-pass")
                          :roles #{:admin}}}
      app (-> (fn [req] {:status 200
                         :headers {"Content-Type" "text/plain"}
                         :body (with-out-str (pr req))})
              (wrap-ratelimit {:limits [(role-limit :admin 20)
                                        (-> 10 limit wrap-limit-user wrap-limit-ip)
                                        (ip-limit 5)]
                               :backend (local-atom-backend)})
              (friend/authenticate {:allow-anon? true
                                    :workflows [(workflows/http-basic
                                                  :credential-fn (partial creds/bcrypt-credential-fn api-users)
                                                  :realm "test-realm")]}))]
  (facts "ratelimit with 3 limiters"
    (fact "uses the admin limit for admins"
      (let [rsp (-> (request :get "/")
                    (header "Authorization" "Basic YWRtaW46YWRtaW4tcGFzcw==")
                    app)]
        (:status rsp) => 200
        (remaining rsp) => "19"))
    (fact "uses the user limit for authenticated requests"
      (let [rsp (-> (request :get "/")
                    (header "Authorization" "Basic YXBpLWtleTphcGktcGFzcw==")
                    app)]
        (:status rsp) => 200
        (remaining rsp) => "9"))
    (fact "uses the composed limit for user-ip"
      (let [rsp (-> (request :get "/")
                    (header "Authorization" "Basic YXBpLWtleTphcGktcGFzcw==")
                    (assoc :remote-addr "host-one")
                    app)]
        (remaining rsp) => "9")
      (let [rsp (-> (request :get "/")
                    (header "Authorization" "Basic YXBpLWtleTphcGktcGFzcw==")
                    (assoc :remote-addr "host-two")
                    app)]
        (remaining rsp) => "9"))
    (fact "uses the ip limit for unauthenticated requests"
      (let [rsp (-> (request :get "/") app)]
        (:status rsp) => 200
        (remaining rsp) => "3"))))
