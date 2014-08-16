(ns yuggoth.layout
  (:require [noir.request :refer :all]
            [yuggoth.config :refer :all]
            [yuggoth.db.core :as db]
            [yuggoth.util :as util]
            [yuggoth.locales :refer [dict]]
            [noir.validation :as vali]
            [noir.session :as session]
            [selmer.parser :as parser]
            [ring.util.response :refer [content-type response]]
            [compojure.response :refer [Renderable]]
            [environ.core :refer [env]]
            [throttler.core :refer [throttle-chan throttle-fn]])
  (:import java.util.Calendar))

(def template-path "templates/")

(def locales (memoize #(map name (keys dict))))

(defn servlet-context [request]
  (when-let [context (:servlet-context request)]
    (try (.getContextPath context)
         (catch IllegalArgumentException e context))))

(deftype RenderableTemplate [template params]
  Renderable
  (render [this request]
    (content-type
      (->> (assoc params
                  :dev (env :dev)
                  :ssl (:ssl @blog-config)
                  :configured @configured?
                  :locales (locales)
                  :admin (boolean (session/get :admin))
                  :site-title (:title (db/get-admin))
                  :servlet-context (servlet-context request))
        (parser/render-file (str template-path template))
        response)
      "text/html; charset=utf-8")))

(defn render-fn [template & [params]]
  (RenderableTemplate. template params))

(def render (throttle-fn render-fn 50 :second))

