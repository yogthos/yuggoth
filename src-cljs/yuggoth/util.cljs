(ns yuggoth.util
  (:import goog.History)
  (:require [yuggoth.session :as session]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [secretary.core :as secretary
             :include-macros true]
            [ajax.core :refer [GET POST]]
            [cljs-time.core :as time]
            [cljs-time.coerce :as time-coerce]
            [cljs-time.format :as time-format]))

(defn error-handler [response]
  (session/reset! {:error response}))

(defn text [id]
  (session/get-in [:locale id]))

(defn hook-browser-navigation!
  "hooks into the browser's navigation (e.g. user clicking on links, redirects, etc) such that any
   of these page navigation events are properly dispatched through secretary so appropriate routing
   can occur. should be called once on app startup"
  []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn format-title-url [id title]
  (if title
    (let [sb (goog.string.StringBuffer.)]
      (doseq [c (.toLowerCase  title)]
        (if (or (= (int c) 32) (and (> (int c) 96) (< (int c) 123)))
          (.append sb c)))
      (str id "-" (js/encodeURI (.toString sb))))))

(defn format-time [time & [fmt]]
  (when time
    (.toTimeString time)
    #_(-> (or fmt "dd MMM, yyyy")
        (time-format/formatter)
        (time-format/unparse time))))

(defn parse-time [time-str & [time-format]]
  time-str
  #_(when time-str
    (-> (or time-format "yyyy-MM-dd HH:mm:ss.SSS")
        (time-format/formatter)
        (time-format/parse time-str))))

(defn link [path & body]
  [:a {:href path} body])

(defn nav-link [path label & [id on-click]]
  [:li {:id id :on-click on-click} (link path (text label))])
