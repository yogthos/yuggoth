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

(defn wrap-ssl-if-selected [app]
  (if (:ssl @blog-config)
    (fn [req]
      (if (or (not-any? #(= (:uri req) (str (:context req) %)) ["/login"])
              (= :https (:scheme req))
              (= "https" ((:headers req) "x-forwarded-proto")))
        (app req)
        (let [host  (-> req
                        (:headers)
                        (get "host")
                        (clojure.string/split #":")
                        (first))
              ssl-port (:ssl-port @blog-config)]
          (resp/redirect (str "https://" host ":" ssl-port (:uri req)) :permanent))))
    app))


(def development-middleware
  [wrap-error-page
   wrap-exceptions])

(def production-middleware
  [#(wrap-internal-error % :log (fn [e] (pprint e)))
   wrap-ssl-if-selected])

(defn load-middleware []
  (concat (when (env :dev) development-middleware)
          production-middleware))

