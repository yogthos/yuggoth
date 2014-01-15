(defproject yuggoth "0.5.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"

  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [lib-noir "0.6.4"]
   [compojure "1.1.5"]
   [ring-server "0.2.8"]
   [clavatar "0.2.1"]
   [clj-rss "0.1.3"]
   [com.taoensso/timbre "1.6.0"]
   [markdown-clj "0.9.25"]
   [net.sf.jlue/jlue-core "1.3"]
   [org.clojure/java.jdbc "0.3.0-alpha4"]
   [postgresql/postgresql "9.1-901.jdbc4"]
   [environ "0.4.0"]]

  :ring
  {:handler yuggoth.handler/war-handler,
   :init yuggoth.handler/init}

  :profiles
  {:production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:dependencies [[ring-mock "0.1.3"] [ring/ring-devel "1.1.8"]]}}

  :plugins [[lein-ring "0.8.3"]])
