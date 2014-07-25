(defproject yuggoth "0.5.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [lib-noir "0.8.4"]
   [compojure "1.1.8"]
   [ring-server "0.3.1"]
   [clavatar "0.2.1"]
   [clj-rss "0.1.8"]
   [com.taoensso/timbre "3.2.1"]
   [markdown-clj "0.9.47"]
   [net.sf.jlue/jlue-core "1.3"]
   [org.clojure/java.jdbc "0.3.4"]
   [postgresql/postgresql "9.1-901-1.jdbc4"]
   [environ "0.5.0"]]

  :ring
  {:handler yuggoth.handler/app,
   :init yuggoth.handler/init}

  :profiles
  {:production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.3.0"]]}}

  :plugins [[lein-ring "0.8.3"] [lein-ancient "0.5.0"]]
  :min-lein-version "2.0.0")
