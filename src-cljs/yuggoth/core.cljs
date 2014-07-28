(ns yuggoth.core
  (:import goog.History)
  (:require [goog.events :as events]
            [clojure.string :as string]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET POST]]
            [secretary.core :as secretary
             :include-macros true :refer [defroute]]
            [yuggoth.routes.home :refer [home-page]]
            [yuggoth.routes.about :refer [about-page]]
            [yuggoth.routes.post :refer [edit-post-page]]
            [yuggoth.routes.archives :refer [archives-page]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [link
                     nav-link
                     text
                     hook-browser-navigation!
                     format-title-url
                     format-time]]))

(enable-console-print!)
(def current-page (atom home-page))

(defn set-page! [page]
  (reset! current-page page))

;;routes
(defroute "/" [] (set-page! home-page))
(defroute "/archives" [] (set-page! archives-page))
(defroute "/last" [] #_(GET "/last-post" {:params {:id (session/get-in [:post :id])}
                                         :handler #(session/put! :post %)})
  (println (str @session/state) #_(session/get-in [:post :id])))
(defroute "/next" [] (GET "/next-post" {:params {:id (session/get-in [:post :id])}
                                         :handler #(session/put! :post %)}))

(defn header []
  [:div.header [:h1 [:div.site-title js/siteTitle]]])

(defn tag-list []
  [:p.taglist
   (for [tag (session/get :tags)]
     [:div.tag (link (str "/tag/" tag) [:span.tagon tag])])])

(defn fetch-archives []
  (GET "/archives" {:handler #(session/put! :archives %)}))

(defn menu []
  [:div.menu
   (into
     (if (session/get :admin)
       [:ul.menu-items
        [nav-link "#/logout" :logout]
        [nav-link "#/profile" :profile]
        [nav-link "#/upload" :upload]
        [nav-link "#/latest-comments" :latest-comments-title "latest"]
        [nav-link "#/make-post" :new-post "new-post"]]
       [:ul.menu-items])
     [[:li#rss [:a {:href "/rss"} [:div#rss "rss"]]]
       [nav-link "#/about" :about-title "about"]
       [nav-link "#/archives" :archives-title "archives" fetch-archives]
       [nav-link "#/" :home-title "home"]])])

(defn sidebar []
  (let [title (session/get :entry-title)]
    (if (or (= (text :new-post) title) (= (text :edit-post) title))
      [:div.sidebar-preview
       [:h2 [:span.render-preview (text :preview-title)]]
       [:div#post-preview]]

      [:div.sidebar
       [:h2 (text :recent-posts-title)]
       (conj
         [:ul
          (for [{:keys [id time title]} (reverse (sort-by :time (session/get :posts)))]
            [:li
             (link (str "#/blog/" (format-title-url id title))
                   title
                   [:div.date (format-time time)])])]
          [nav-link "#/archives" (text :more)])
       (tag-list)])))

(defn page []
  [:div.container
   [header]
   [menu]
   [:div.contents
     [@current-page]
     [sidebar]]])

(defn parse-post-id [url]
  (let [[x y] (clojure.string/split url #":\d+")]
    (re-find #"\d+" (or y x))))

(defn init []
  (secretary/set-config! :prefix "#")
  (hook-browser-navigation!)
  (session/init!)
  (let [[_ uri] (.split clojure.string (.-URL js/document) #"\#")]
    (set-page! (or (get {"/archives" archives-page
                         "/about" about-page}
                        uri)
                   home-page)))
  (fetch-archives)
  (GET "/locale" {:handler #(session/put! :locale %)})
  (if-let [blog-id (parse-post-id (.-URL js/document))]
    (GET "/blog-post" {:params {:id blog-id}
                       :handler #(session/put! :post %)})
    (GET "/latest-post" {:handler #(session/put! :post %)}))
  (GET "/posts/5" {:handler #(session/put! :posts %)})
  (GET "/tags" {:handler #(session/put! :tags %)})

  (reagent/render-component
    [page]
    (.getElementById js/document "app")))

(init)





