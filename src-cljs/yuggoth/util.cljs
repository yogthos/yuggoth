(ns yuggoth.util
  (:import goog.History)
  (:require [yuggoth.session :as session]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [clojure.string :refer [join]]
            [secretary.core :as secretary
             :include-macros true]
            [ajax.core :as ajax]))

(defn auth-hash [user pass]
  (->> (str user ":" pass) (js/btoa) (str "Basic ")))

(defn GET [url params]
  (ajax/GET (str js/context url) params))

(defn POST [url params]
  (ajax/POST (str js/context url) params))

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
  (when title
    (->> (re-seq #"[a-zA-Z0-9]+" title)
         (clojure.string/join "-")
         (js/encodeURI)
         (str id "-"))))

(defn url [parts]
  (if-let [context (not-empty js/context)]
    (apply (partial str context "/") parts)
    (apply str parts)))

(defn set-location! [& url-parts]
  (set! (.-href js/location) (url url-parts)))

(defn set-post-url [{:keys [id]}]
  (set-location! "#/blog/" id))

(defn set-page! [page]
  (session/put! :current-page page))

(defn set-admin-page! [page]
  (if (session/get :admin)
    (set-page! page)
    (set-location! "/")))

(defn set-title! [title]
  (set! (.-title js/document) title))

(defn set-recent! []
  (GET "/posts/5" {:handler #(session/put! :recent-posts %)}))

(defn set-current-post! [post]
  (session/put! :post post)
  (set-recent!)
  (GET "/tags" {:handler #(session/put! :tags %)}))

(defn fetch-post [id handler]
  (fn []
    (GET "/blog-post"
         {:params {:id id}
          :handler handler})))

(defn html [content]
  (let [element (.createElement js/document "div")]
    (set! (.-innerHTML element) content)
    (let [nodes (.querySelectorAll element "pre code")]
      (loop [i (.-length nodes)]
        (when-not (neg? i)
          (when-let [item (.item nodes i)]
            (.highlightBlock js/hljs item))
          (recur (dec i)))))
    {:dangerouslySetInnerHTML
      {:__html (.-innerHTML element)}}))

(defn markdown [text]
  (-> text str js/marked html))

(defn input-value [input]
  (-> input .-target .-value))

(defn set-value! [target]
  (fn [source] (reset! target (input-value source))))

(defn text-input [target & [opts]]
  [:input (merge
           {:type "text"
            :on-change (set-value! target)
            :value @target}
           opts)])

(defn link [& [x y & xs :as body]]
  (if (map? x)
    [:a (merge {:href (url y)} x) xs]
    [:a {:href (url x)} (rest body)]))

(defn nav-link [path label & [on-click]]
  [:li {:on-click on-click} (link path (text label))])
