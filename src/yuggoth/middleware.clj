(ns yuggoth.middleware
    (:require
      [yuggoth.config :refer [blog-config]]
      [selmer.parser :as parser]
      [environ.core :refer [env]]
      [clojure.pprint :refer [pprint]]
      [selmer.middleware :refer [wrap-error-page]]
      [noir.response :as resp]
      [noir-exception.core
        :refer [wrap-internal-error wrap-exceptions]]))

(def development-middleware
  [wrap-error-page
   wrap-exceptions])

(def production-middleware
  [#(wrap-internal-error % :log (fn [e] (pprint e)))])

(defn load-middleware []
  (concat (when (env :dev) development-middleware)
          production-middleware))

