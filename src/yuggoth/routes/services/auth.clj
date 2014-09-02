(ns yuggoth.routes.services.auth
  (:require [yuggoth.db.core :as db]
            [yuggoth.config :refer [text]]
            [noir.session :as session]
            [noir.util.crypt :as crypt])
  (:import org.apache.commons.codec.binary.Base64
           java.nio.charset.Charset))

(defn auth [req]
  (get-in req [:headers "authorization"]))

(defn decode-auth [encoded]
  (let [auth (second (.split encoded " "))]
    (-> (Base64/decodeBase64 auth)
        (String. (Charset/forName "UTF-8"))
        (.split ":"))))

(defn login! [req]
  (let [admin (db/get-admin)
        [user pass] (decode-auth (auth req))]
    (if (and (= user (:handle admin))
             (crypt/compare pass (:pass admin)))
      (do (session/put! :admin admin)
          {:result "ok"})
      {:error (text :wrong-password)})))

(defn logout! []
  (session/clear!)
  {:result "ok"})
