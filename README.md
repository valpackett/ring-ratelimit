# ring-ratelimit

Ring middleware for request rate limiting.

## Usage

```clojure
(ns your.app
  (:use [ring.middleware ratelimit]))

(def app (-> your-routes-or-whatever
             (wrap-ratelimit {:limit 100})))
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