(ns yuggoth.components.login
 (:require [secretary.core :as secretary
             :include-macros true]
            [reagent.core :as reagent :refer [atom]]
            [yuggoth.pages.home :refer [home-page]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [GET
                     POST
                     auth-hash
                     text
                     text-input
                     set-location!]]))

;;TODO: change login to a modal!
(defn login! [user pass error]
  (cond
    (empty? user)
    (reset! error (text :admin-required))
    (empty? pass)
    (reset! error (text :admin-pass-required))
    :else
    (POST "/login" {:headers {"Authorization" (auth-hash user pass)}
                    :handler #(if (= (:result %) "ok")
                                (do
                                  (session/remove! :login)
                                  (session/put! :admin true))
                                (reset! error (:error %)))})))

(defn login-form []
  (let [user (atom nil)
        pass (atom nil)
        error (atom nil)]
    (fn []
      [:div.login-form
        [text-input user {:placeholder (text :user)}]
        [text-input pass {:type "password" :placeholder (text :password)}]
       [:span.button.login-button {:on-click #(session/remove! :login)} (text :cancel)]
        [:span.button.login-button {:on-click #(login! @user @pass error)} (text :login)]
        (if-let [error @error]
          [:div.error error])])))
