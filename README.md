# ring-ratelimit

Ring middleware for request rate limiting.

## Usage

```clojure
(ns your.app
  (:use [ring.middleware ratelimit]))

(def app (-> your-routes-or-whatever
             (wrap-ratelimit {:limit 100})))
; 100 per hour per IP address


; You can use a custom handler for the error (when the user has no requests left):
(def app (-> your-routes-or-whatever
             (wrap-ratelimit
               {:limit 100
                :err-handler (fn [req] {:status 429
                                        :headers {"Content-Type" "text/html"}
                                        :body (your-error-template {:text "Too many requests"})})})))
```

## License

           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                   Version 2, December 2004

Copyright (C) 2013 Greg V

Everyone is permitted to copy and distribute verbatim or modified
copies of this license document, and changing it is allowed as long
as the name is changed.

           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
  TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

 0. You just DO WHAT THE FUCK YOU WANT TO.
