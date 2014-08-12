(ns yuggoth.routes.services.profile
  (:require [yuggoth.db.core :as db]
            [yuggoth.config :refer [text]]
            [noir.session :as session]
            [noir.util.crypt :as crypt]
            [yuggoth.routes.services.auth :refer [decode-auth]]))

(defn profile []
  (select-keys (db/get-admin) [:title :about :handle :email]))

(defn get-tags []
  (db/tags))

(defn set-title! [params]
  (db/update-admin! (session/get-in [:admin :handle]) params))

(defn set-handle! [params]
  (let [result (db/update-admin! (session/get-in [:admin :handle]) params)]
    (when (= [1] result)
      (session/assoc-in! [:admin :handle] (:handle params))
      result)))

(defn set-email! [params]
  (db/update-admin! (session/get-in [:admin :handle]) params))

(defn set-about! [params]
  (db/update-admin! (session/get-in [:admin :handle]) params))

(defn change-password! [auth {:keys [pass repeat-pass]}]
  (let [admin (db/get-admin)
        [user old-pass] (decode-auth auth)]
    (cond
     (not= user (:handle admin))
     (throw (Exception. (text :unkown-user)))
     (not (crypt/compare old-pass (:pass admin)))
     (throw (Exception. (text :wrong-password)))
     (not= pass repeat-pass)
     (throw (Exception. (text :pass-mismatch)))
     (empty? pass)
     (throw (Exception. (text :password-empty)))
     :else
     (db/update-admin! user {:pass (crypt/encrypt pass)}))))
