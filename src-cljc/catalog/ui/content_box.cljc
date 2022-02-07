(ns catalog.ui.content-box
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.platform.component-utils :as utils]))

(defcomponent organism
  [{:keys [title header summary sections]} _ _]
  [:div.py8.px4.bg-cool-gray
   [:div.max-960.mx-auto
    [:div.pb2
     [:div.proxima.title-2.bold.caps ^:inline (str title)]
     [:div.canela.title-1.pb2 ^:inline (str header)]
     [:div.canela.content-1 ^:inline (str summary)]]

    (for [{:keys [title body]} sections]
      [:div.py2 {:key title}
       [:div.proxima.title-2.bold.caps.pb1 ^:inline (str title)]
       [:div.canela.content-2
        (map-indexed
         (fn [j {:keys [text nav-message external-uri]}]
           (cond
             nav-message
             [:a.p-color (assoc (apply utils/route-to nav-message) :key (str "text-" j))
              (str text " ")]

             external-uri
             [:a.p-color {:href external-uri :key (str "text-" j)}
              (str text " ")]

             :else
             [:span {:key (str "text-" j)} (str text " ")]))
         body)]])

    [:div.py2
     [:div.proxima.title-2.bold.caps.pb1 "Still Have Questions?"]
     [:div.canela.content-2
      [:div "Customer Service can help!"]
      [:div "Call " (ui/link :link/phone :a.inherit-color {} config/support-phone-number)]
      [:div "Monday through Friday from 8am-5pm PST."]]]]])
