(ns yuggoth.pages.home
  (:require [ajax.core :refer [GET POST]]
            [secretary.core :as secretary
             :include-macros true]
            [reagent.core :as reagent :refer [atom]]
            [yuggoth.session :as session]
            [markdown.core :refer [md->html]]
            [yuggoth.util
             :refer [text
                     link]]))

(defn set-current-post [post]
  (session/put! :post post)
  (set! (.-href window.location) (str "/#/blog/" (:id post))))

(defn fetch-post [next?]
  (GET "/blog-post"
       {:params {:id ((if next? inc dec) (session/get-in [:post :id]))}
                 :handler set-current-post}))

(defn toggle-post [content post-id public]
  )

(defn admin-forms [post-id visible]
  (when (session/get :admin)
    [:div
     [:div [:span.submit
            {:on-click #(do
                         (js/alert "toggle")
                         #_(POST "/toggle-post" {:params {:post-id post-id
                                                 :public visible}}))}
            (if visible (text :hide) (text :show))]]

     [:div [:span.submit
            {:on-click #(do
                         (js/alet "edit")
                         #_(POST "/update-post" {:params {:post-id post-id}}))}
            (text :edit)]]]))

(defn post-nav []
  [:div
   [:div.leftmost.comment-preview
    [:a.tagon {:on-click #(fetch-post false)}
      (text :previous)]]
   [:div.rightmost.comment-preview
    [:a.tagon {:on-click #(fetch-post true)}
      (text :next)]]])

(defn home-page []
  (let [{:keys [content time public title author id]}
        (session/get :post)]
    [:div.post
     [:div.entry-title [:h2 title ]]
     [:div.entry-content
      {:dangerouslySetInnerHTML
           {:__html (md->html (str content))}}]
     [post-nav]]))
