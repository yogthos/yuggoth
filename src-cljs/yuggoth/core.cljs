(ns yuggoth.core
  (:import goog.History)
  (:require [goog.events :as events]
            [clojure.string :as string]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent :refer [atom]]
            [secretary.core :as secretary
             :include-macros true :refer [defroute]]
            [yuggoth.noise :refer [make-noise set-body-background]]
            [yuggoth.pages.home :refer [home-page]]
            [yuggoth.pages.upload :refer [upload-page]]
            [yuggoth.pages.latest-comments :refer [latest-comments-page]]
            [yuggoth.pages.profile :refer [profile-page]]
            [yuggoth.pages.about :refer [about-page]]
            [yuggoth.pages.post :refer [edit-post-page make-post-page]]
            [yuggoth.pages.archives :refer [archives-page]]
            [yuggoth.components.login :refer [login-form]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [GET
                     POST
                     link
                     nav-link
                     text
                     hook-browser-navigation!
                     format-title-url
                     fetch-post
                     set-current-post!
                     set-page!
                     set-admin-page!]]))

;(enable-console-print!)

(defn fetch-archives! [& [tag]]
  (GET (if tag (str "/tag/" tag) "/archives")
       {:handler #(do (session/put! :archives %)
                      (session/put! :archives-tag tag)
                      (set-page! archives-page))}))

;;routes
(defroute "/" []
  (set-page! home-page)
  (GET "/latest-post" {:handler set-current-post!}))
(defroute "/about" [] (set-page! about-page))
(defroute "/archives" [] (fetch-archives!))
(defroute "/latest-comments" [] (set-page! latest-comments-page))
(defroute "/upload" [] (set-admin-page! upload-page))
(defroute "/profile" [] (set-admin-page! profile-page))
(defroute "/make-post" [] (set-admin-page! make-post-page))
(defroute "/edit-post" [] (set-admin-page! edit-post-page))
(defroute "/tag/:tag" [tag] (fetch-archives! tag))
(defroute "/login" [] (session/put! :login true))

(defn header []
  [:div.header [:h1 [:div.site-title js/siteTitle]]])

(defn footer []
  [:div.footer
   [:p (str "Copyright © 2012-" (.getFullYear (js/Date.)) " ")
    (session/get-in [:profile :handle])
    (when-not (session/get :admin) [:span " (" [:a {:on-click #(secretary/dispatch! "#/login")} #_{:href "#/login"} (text :login)] ")"])
    (text :powered-by)
    [:a {:href "http://github.com/yogthos/yuggoth"} "Yuggoth"]]])

(defn logout! []
  (session/remove! :admin)
  (set-page! home-page))

(defn menu []
  [:div.menu
   (into
    (if (session/get :admin)
      [:ul.menu-items
        [:li {:on-click
              #(GET "/logout" {:handler logout!})}
         [:a (text :logout)]]
        [nav-link "#/profile" :profile]
        [nav-link "#/upload" :upload]
        [nav-link "#/latest-comments" :latest-comments-title]
        [nav-link "#/make-post" :new-post]]
      [:ul.menu-items])
    [[:li#rss [:a {:href (str js/context "/rss")} [:span#rss "rss"]]]
     [nav-link "#/about" :about-title]
     [nav-link "#/archives" :archives-title]
     [nav-link "#/" :home-title]])])

(defn page []
  [:div.container
   [header]
   [menu]
   (if-let [current-page (session/get :current-page)]
     [current-page]
     [:div.contents [:div.post (text :loading)]])
   (if (session/get :login)
     [login-form]
     [footer])])

(defn parse-post-id [url]
  (let [[x y] (clojure.string/split url #":\d+")]
    (re-find #"\d+" (or y x))))

(defn set-post-and-home-page! [result]
  (set-current-post! result)
  (set-page! home-page))

(defn generate-background []
  (when-not (session/get :mobile?)
    (set-body-background
     (make-noise
      :opacity 1
      :width 80
      :height 80
      :from-color 0xdfdfdf
      :to-color 0xefefef))))

(defn init! []
  (session/put!
   :mobile?
   (boolean
    (re-find #"Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini"
             (.-userAgent js/navigator))))
  (when js/admin (session/put! :admin true))
  (secretary/set-config! :prefix "#")
  (hook-browser-navigation!)
  ;;fetch initial data
  (GET "/profile" {:handler #(session/put! :profile %)})
  (GET "/locale" {:handler #(session/put! :locale %)})

  ;; set the appropriate page based on the URL
  (let [[_ uri] (.split clojure.string (.-URL js/document) #"\#")]
    (set-page! (get {"/archives" archives-page
                     "/about" about-page
                     "/make-post" make-post-page
                     "/upload" upload-page}
                    uri
                    home-page)))
  ;;fetch the post based on the URL
  (if-let [post-id (parse-post-id (.-URL js/document))]
    ((fetch-post post-id set-post-and-home-page!))
    (GET "/latest-post" {:handler set-current-post!}))

  ;;render the page
  (reagent/render-component
    [page]
    (.getElementById js/document "app"))
  ;;create background noise
  (generate-background))

(init!)





