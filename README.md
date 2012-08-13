# Yuggoth

<img src="https://raw.github.com/yogthos/yuggoth/master/logo.jpg" style="margin-left:50px;"
 hspace="20"
 alt="Yuggoth logo" title="a strange dark orb" align="right" width="200" height="200"/>
 
>"Yuggoth... is a strange dark orb at the very rim of our solar system... 
>There are mighty cities on Yuggoth—great tiers of terraced towers built of black stone... 
>The sun shines there no brighter than a star, but the beings need no light. 
>They have other subtler senses, and put no windows in their great houses and temples..."

> — H. P. Lovecraft, &quot;The Whisperer in Darkness&quot;


Yuggoth is a blog engine which powers my site at http://yogthos.net and allows me to experiment with using Noir.   

## Features

* content caching
* RSS feed
* tags
* markdown in posts and comments with live preview
* syntax highlighting using [syntax highlighter](http://alexgorbatchev.com/SyntaxHighlighter/)
* file uploads and management through web UI
* custom styles
* captchas for comments
* view latest comments
* toggle post visibility

## Usage

First, setup postgreSQL and update `db` definition in `yuggoth.models.db` to point to it: 
```clojure
(def ^{:private true} db
  {:datasource (doto (new PGPoolingDataSource)
                 (.setServerName "localhost")
                 (.setDatabaseName "yourdb")
                 (.setUser "user")
                 (.setPassword "pass")
                 (.setMaxConnections 10))})
```
The blog will automatically create the necessary tables for you. 
When you navigate to the blog on the first run it will present the setup wizard wich will allow you to configure the administrator and the blog title.
Further configuration can be done on the profile page.   

If you use cake, substitute 'lein' with 'cake' below. Everything should work fine.

```bash
lein deps
lein run
```

to enable SSL uncomment `secure-login-redirect` in `service.clj` and optionally change the port to the one you're using, default is 443
```clojure
(defn secure-login-redirect [handler]
  (fn [request]
    (let [{:keys [scheme uri server-name server-port]} request]
      (if (and (= scheme :http) (or (.contains uri "login") (.contains uri "create-admin")))
        (ring.util.response/redirect (str "https://" server-name ":443" uri)) ;change SSL port if needed
        (handler request)))))
        
(def handler
  (-> (server/gen-handler
        {:mode :prod,
         :ns 'yuggoth
         :session-cookie-attrs {:max-age 1800000}})
   
    ;;enable this to redirect login to HTTPS
    ;;make sure that the container has an HTTPS listener setup
    ;;if you're listening on a non standard SSL port (not 443), you will have to change the port above
    ;;I haven't found a way to get the port from the container
    ;secure-login-redirect
    fix-base-url))        
```

to run as standalone
```bash
lein uberjar
java -jar yuggoth-0.4.0-SNAPSHOT-standalone.jar
```

valid options for running standalone

* -mode dev or prod
* -port integer

to make a deployable WAR
```bash
lein ring uberwar
```



see [markdown-clj](https://github.com/yogthos/markdown-clj) for supported syntax in posts, any valid HTML will work as well

 
## License

Distributed under the Eclipse Public License, the same as Clojure.

***
Copyright (C) 2012 Yogthos

