(defproject ring-ratelimit "0.2.0-SNAPSHOT"
  :description "Rate limit middleware for Ring"
  :url "https://github.com/myfreeweb/ring-ratelimit"
  :license {:name "WTFPL"
            :url "http://www.wtfpl.net/about/"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :profiles {:dev {:dependencies [[midje "1.5-beta1"]
                                  [lein-release "1.0.0"]
                                  [ring-mock "0.1.3"]
                                  [com.cemerick/friend "0.1.3"]
                                  [com.taoensso/carmine "1.3.0"]]}}
  :plugins [[lein-midje "3.0-alpha4"]
            [lein-release "1.0.0"]]
  :bootclasspath true
  :lein-release {:deploy-via :lein-deploy}
  :repositories [["snapshots" {:url "https://clojars.org/repo" :creds :gpg}]
                 ["releases"  {:url "https://clojars.org/repo" :creds :gpg}]])
