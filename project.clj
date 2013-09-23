(defproject yuggoth "0.5.0-SNAPSHOT"

  :description "Yuggoth, a pure Clojure blogging engine"
  :url "http://github.com/yogthos/yuggoth"

  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [lib-noir "0.6.9"]
   [compojure "1.1.5"]
   [ring-server "0.3.0"]
   [clj-rss "0.1.3"]
   [selmer "0.4.2"]
   [com.taoensso/timbre "2.6.1"]
   [markdown-clj "0.9.32"]
   [net.sf.jlue/jlue-core "1.3"]
   [org.clojure/java.jdbc "0.3.0-alpha4"]
   [postgresql/postgresql "9.1-901.jdbc4"]
   [clj-time "0.4.3"]]

  :ring
  {:handler yuggoth.handler/war-handler, 
   :init yuggoth.handler/init}

  :profiles
  {:production 
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev 
   {:dependencies [[ring-mock "0.1.3"] [ring/ring-devel "1.1.8"]]}}

  :plugins [[lein-ring "0.8.7"]])
