(ns yuggoth.server
  (:use hiccup.middleware)
  (:require [noir.server :as server]
            [yuggoth.views archives auth blog common upload]))

(server/load-views-ns 'yuggoth.views)
(def handler (wrap-base-url (server/gen-handler {:mode :prod, :ns 'yuggoth})))

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'yuggoth})))
