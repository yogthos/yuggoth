(ns yuggoth.routes.services.upload
  (:require [yuggoth.util :as util]
            [yuggoth.db.core :as db]
            [noir.response :as resp]
            [noir.session :as session]))

(defmacro in-try-catch [& body]
  `(try
     ~@(butlast body)
     (resp/status 200 ~(last body))
     (catch Throwable t#
       (.printStackTrace t#)
       (resp/status 500 "error"))))

(defn list-files []
  (db/list-files))

(defn get-file [name]
  (if-let [{:keys [name type data]} (db/get-file name)]
    (resp/content-type type (new java.io.ByteArrayInputStream data))
    (resp/status 404 (resp/empty))))

(defn upload-file! [file]
  (in-try-catch
   (db/store-file! file)
   (:filename file)))

(defn delete-file! [name]
  (in-try-catch (db/delete-file! name)))
